import * as logger from "firebase-functions/logger";

/**
 * Internal boundary for backend operational/analytics events.
 * This keeps transport concerns out of business services and allows a
 * Measurement Protocol implementation to replace/augment structured logs
 * without touching callable handlers.
 */
export class AnalyticsService {
  info(event: string, metadata: Record<string, unknown> = {}): void {
    logger.info(event, metadata);
  }

  error(event: string, metadata: Record<string, unknown> = {}): void {
    logger.error(event, metadata);
  }
}
