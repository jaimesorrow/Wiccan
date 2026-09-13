import { Firestore } from "firebase-admin/firestore";
import { SystemConfig, SystemConfigPort } from "./systemConfigPort";

const SYSTEM_CONFIG_COLLECTION = "systemConfig";
const GLOBAL_CONFIG_DOC = "global";

export class FirestoreSystemConfigRepository implements SystemConfigPort {
  constructor(private readonly db: Firestore) {}

  async getConfig(): Promise<SystemConfig> {
    const doc = await this.db.collection(SYSTEM_CONFIG_COLLECTION).doc(GLOBAL_CONFIG_DOC).get();
    return {
      dailyContentEnabled: doc.get("dailyContentEnabled") !== false,
      notificationsEnabled: doc.get("notificationsEnabled") !== false,
    };
  }
}
