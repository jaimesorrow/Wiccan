import { isValidTimeZone } from "./dateUtils";

export const MAX_DEVICE_TOKEN_LENGTH = 4096;
export const MAX_TIME_ZONE_LENGTH = 255;

/**
 * Accept only a bounded, non-blank FCM registration token. Rejecting
 * surrounding whitespace avoids creating two Firestore device records for
 * what should be one token, while the length cap prevents oversized callable
 * payloads from reaching hashing and Firestore writes.
 */
export function parseDeviceToken(value: unknown): string | null {
  if (typeof value !== "string" || value.length === 0 || value.length > MAX_DEVICE_TOKEN_LENGTH) {
    return null;
  }
  if (value !== value.trim() || /\s/.test(value)) {
    return null;
  }
  return value;
}

/** Accept only a bounded IANA zone or supported Android GMT/UTC offset. */
export function parseTimeZone(value: unknown): string | null {
  if (typeof value !== "string" || value.length === 0 || value.length > MAX_TIME_ZONE_LENGTH) {
    return null;
  }
  if (value !== value.trim() || !isValidTimeZone(value)) {
    return null;
  }
  return value;
}
