import { SystemConfig, SystemConfigPort } from "../systemConfigPort";

export class FakeSystemConfigRepository implements SystemConfigPort {
  config: SystemConfig = {
    dailyContentEnabled: true,
    notificationsEnabled: true,
    contentGenerationEnabled: false,
    contentGenerationBatchSize: 3,
  };

  async getConfig(): Promise<SystemConfig> {
    return this.config;
  }
}
