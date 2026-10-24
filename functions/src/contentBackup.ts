import { createHash } from "node:crypto";
import { gunzipSync, gzipSync } from "node:zlib";
import { DocumentReference, Firestore, GeoPoint, Timestamp } from "firebase-admin/firestore";

const TYPE_KEY = "__wiccanFirestoreType";
const FORMAT_VERSION = 1;

type EncodedValue = null | boolean | string | number | { [TYPE_KEY]: string; [key: string]: unknown };

export interface ContentBackupDocument {
  id: string;
  createTime: string;
  updateTime: string;
  data: EncodedValue;
}
export interface ContentBackupArtifact {
  formatVersion: number;
  projectId: string;
  databaseId: "(default)";
  collectionPath: "content";
  completedAt: string;
  documents: ContentBackupDocument[];
}

function tagged(type: string, fields: Record<string, unknown> = {}): EncodedValue {
  return { [TYPE_KEY]: type, ...fields };
}

export function encodeFirestoreValue(value: unknown): EncodedValue {
  if (value === null || typeof value === "boolean" || typeof value === "string") return value;
  if (typeof value === "number") {
    if (Number.isFinite(value)) return value;
    return tagged("number", { value: String(value) });
  }
  if (value instanceof Timestamp) return tagged("timestamp", { seconds: value.seconds, nanoseconds: value.nanoseconds });
  if (value instanceof Date) return tagged("date", { value: value.toISOString() });
  if (value instanceof GeoPoint) return tagged("geopoint", { latitude: value.latitude, longitude: value.longitude });
  if (value instanceof DocumentReference) return tagged("reference", { path: value.path });
  if (Buffer.isBuffer(value) || value instanceof Uint8Array) return tagged("bytes", { base64: Buffer.from(value).toString("base64") });
  if (Array.isArray(value)) return tagged("array", { values: value.map(encodeFirestoreValue) });
  if (value && typeof value === "object") {
    const prototype = Object.getPrototypeOf(value);
    if (prototype !== Object.prototype && prototype !== null) throw new Error(`Unsupported Firestore value '${prototype?.constructor?.name ?? "unknown"}'.`);
    const entries = Object.fromEntries(Object.entries(value as Record<string, unknown>)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, child]) => [key, encodeFirestoreValue(child)]));
    return tagged("map", { entries });
  }
  throw new Error(`Unsupported Firestore value type '${typeof value}'.`);
}

function objectValue(value: EncodedValue): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Malformed tagged Firestore value.");
  return value as Record<string, unknown>;
}

export function decodeFirestoreValue(value: EncodedValue, db: Firestore): unknown {
  if (value === null || typeof value === "boolean" || typeof value === "string" || typeof value === "number") return value;
  const record = objectValue(value);
  switch (record[TYPE_KEY]) {
    case "number": {
      if (record.value === "NaN") return Number.NaN;
      if (record.value === "Infinity") return Number.POSITIVE_INFINITY;
      if (record.value === "-Infinity") return Number.NEGATIVE_INFINITY;
      throw new Error("Malformed non-finite number.");
    }
    case "timestamp":
      if (!Number.isInteger(record.seconds) || !Number.isInteger(record.nanoseconds)) throw new Error("Malformed timestamp.");
      return new Timestamp(record.seconds as number, record.nanoseconds as number);
    case "date":
      if (typeof record.value !== "string" || !Number.isFinite(Date.parse(record.value))) throw new Error("Malformed date.");
      return new Date(record.value);
    case "geopoint":
      if (typeof record.latitude !== "number" || typeof record.longitude !== "number") throw new Error("Malformed geopoint.");
      return new GeoPoint(record.latitude, record.longitude);
    case "reference":
      if (typeof record.path !== "string" || !record.path || record.path.startsWith("/") || record.path.endsWith("/")) throw new Error("Malformed reference.");
      return db.doc(record.path);
    case "bytes":
      if (typeof record.base64 !== "string") throw new Error("Malformed bytes.");
      return Buffer.from(record.base64, "base64");
    case "array":
      if (!Array.isArray(record.values)) throw new Error("Malformed array.");
      return (record.values as EncodedValue[]).map((child) => decodeFirestoreValue(child, db));
    case "map": {
      if (!record.entries || typeof record.entries !== "object" || Array.isArray(record.entries)) throw new Error("Malformed map.");
      return Object.fromEntries(Object.entries(record.entries as Record<string, EncodedValue>)
        .map(([key, child]) => [key, decodeFirestoreValue(child, db)]));
    }
    default:
      throw new Error("Unknown tagged Firestore value.");
  }
}

export async function captureContentBackup(db: Firestore, projectId: string, completedAt = new Date().toISOString()): Promise<ContentBackupArtifact> {
  const snapshot = await db.collection("content").get();
  const documents = snapshot.docs.map((document) => ({
    id: document.id,
    createTime: document.createTime.toDate().toISOString(),
    updateTime: `${document.updateTime.seconds}:${document.updateTime.nanoseconds}`,
    data: encodeFirestoreValue(document.data()),
  })).sort((left, right) => left.id.localeCompare(right.id));
  return { formatVersion: FORMAT_VERSION, projectId, databaseId: "(default)", collectionPath: "content", completedAt, documents };
}

export function serializeContentBackup(artifact: ContentBackupArtifact): Buffer {
  validateContentBackup(artifact);
  return gzipSync(Buffer.from(JSON.stringify(artifact) + "\n", "utf8"), { level: 9 });
}

export function parseContentBackup(bytes: Buffer): ContentBackupArtifact {
  const parsed: unknown = JSON.parse(gunzipSync(bytes).toString("utf8"));
  validateContentBackup(parsed);
  return parsed as ContentBackupArtifact;
}

export function contentBackupSha256(bytes: Buffer): string {
  return createHash("sha256").update(bytes).digest("hex");
}

function validateEncodedValue(value: unknown): void {
  if (value === null || typeof value === "boolean" || typeof value === "string" || typeof value === "number") return;
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Backup contains an invalid encoded value.");
  const record = value as Record<string, unknown>;
  switch (record[TYPE_KEY]) {
    case "number":
      if (!["NaN", "Infinity", "-Infinity"].includes(String(record.value))) throw new Error("Backup contains an invalid encoded number.");
      return;
    case "timestamp":
      if (!Number.isInteger(record.seconds) || !Number.isInteger(record.nanoseconds)) throw new Error("Backup contains an invalid timestamp.");
      return;
    case "date":
      if (typeof record.value !== "string" || !Number.isFinite(Date.parse(record.value))) throw new Error("Backup contains an invalid date.");
      return;
    case "geopoint":
      if (typeof record.latitude !== "number" || typeof record.longitude !== "number") throw new Error("Backup contains an invalid geopoint.");
      return;
    case "reference":
      if (typeof record.path !== "string" || !record.path || record.path.startsWith("/") || record.path.endsWith("/")) throw new Error("Backup contains an invalid reference.");
      return;
    case "bytes":
      if (typeof record.base64 !== "string") throw new Error("Backup contains invalid bytes.");
      return;
    case "array":
      if (!Array.isArray(record.values)) throw new Error("Backup contains an invalid array.");
      record.values.forEach(validateEncodedValue);
      return;
    case "map":
      if (!record.entries || typeof record.entries !== "object" || Array.isArray(record.entries)) throw new Error("Backup contains an invalid map.");
      Object.values(record.entries as Record<string, unknown>).forEach(validateEncodedValue);
      return;
    default:
      throw new Error("Backup contains an unknown encoded value.");
  }
}

export function validateContentBackup(value: unknown): asserts value is ContentBackupArtifact {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Backup artifact must be an object.");
  const artifact = value as Partial<ContentBackupArtifact>;
  if (artifact.formatVersion !== FORMAT_VERSION || artifact.databaseId !== "(default)" || artifact.collectionPath !== "content" ||
      typeof artifact.projectId !== "string" || !artifact.projectId || typeof artifact.completedAt !== "string" || !Number.isFinite(Date.parse(artifact.completedAt)) || !Array.isArray(artifact.documents)) {
    throw new Error("Backup artifact metadata is invalid.");
  }
  const ids = new Set<string>();
  for (const document of artifact.documents) {
    if (!document || typeof document.id !== "string" || !document.id || document.id.includes("/") || ids.has(document.id) ||
        typeof document.createTime !== "string" || !Number.isFinite(Date.parse(document.createTime)) ||
        typeof document.updateTime !== "string" || !/^\d+:\d{1,9}$/.test(document.updateTime)) {
      throw new Error("Backup artifact contains an invalid or duplicate document.");
    }
    ids.add(document.id);
    validateEncodedValue(document.data);
  }
}
