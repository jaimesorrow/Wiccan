import { TrialReminderCandidate, TrialReminderRepositoryPort } from "../trialReminderPort";

export class FakeTrialReminderRepository implements TrialReminderRepositoryPort {
  trials: TrialReminderCandidate[] = [];
  /** uid -> trial expiry a reminder was already claimed for. */
  claimed = new Map<string, number>();

  async listTrialsEndingBetween(afterMillis: number, untilMillis: number): Promise<TrialReminderCandidate[]> {
    return this.trials.filter((trial) =>
      trial.expiryTimeMillis > afterMillis && trial.expiryTimeMillis <= untilMillis &&
      this.claimed.get(trial.uid) !== trial.expiryTimeMillis);
  }

  async claimReminder(uid: string, purchaseToken: string, expiryTimeMillis: number): Promise<boolean> {
    const trial = this.trials.find((candidate) => candidate.uid === uid);
    if (!trial || trial.purchaseToken !== purchaseToken) return false;
    if (this.claimed.get(uid) === expiryTimeMillis) return false;
    this.claimed.set(uid, expiryTimeMillis);
    return true;
  }
}
