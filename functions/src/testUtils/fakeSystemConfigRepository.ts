import { SystemConfig, SystemConfigPort } from "../systemConfigPort";

export class FakeSystemConfigRepository implements SystemConfigPort {
  config: SystemConfig = { dailyContentEnabled: true, notificationsEnabled: true };

  async getConfig(): Promise<SystemConfig> {
    return this.config;
  }
}
