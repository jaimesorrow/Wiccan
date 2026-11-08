package com.jaimesorrow.wiccan.ui.legal

import com.jaimesorrow.wiccan.domain.model.PolicyVersions

enum class LegalDocumentId {
    TERMS,
    PRIVACY,
    CONTENT_SAFETY,
}

data class LegalSection(
    val heading: String,
    val paragraphs: List<String>,
)

data class LegalDocument(
    val id: LegalDocumentId,
    val title: String,
    val version: Int?,
    val effectiveDate: String,
    val introduction: String,
    val sections: List<LegalSection>,
)

/**
 * Native, versioned policy copy that stays available at account creation,
 * policy acceptance, subscription purchase, and from Settings. The same text
 * is published at PolicyUrls (hosting/); LegalDocumentsTest fails if the
 * hosted pages drift from it.
 */
object WiccanLegalDocuments {
    fun get(id: LegalDocumentId): LegalDocument = when (id) {
        LegalDocumentId.TERMS -> terms
        LegalDocumentId.PRIVACY -> privacy
        LegalDocumentId.CONTENT_SAFETY -> contentSafety
    }

    val terms = LegalDocument(
        id = LegalDocumentId.TERMS,
        title = "Terms of Service",
        version = PolicyVersions.TERMS,
        effectiveDate = "November 7, 2026",
        introduction =
            "These Terms of Service (“Terms”) are an agreement between you and Jaime Sorrow (“Wiccan,” “we,” or “us”) " +
                "and govern your use of the Wiccan app and related services. By accepting them, you agree to use the app " +
                "responsibly and in accordance with applicable law.",
        sections = listOf(
            LegalSection(
                "What Wiccan provides",
                listOf(
                    "Wiccan presents educational, historical, and spiritual-reflection content inspired by ancient grimoires " +
                        "and related primary sources. It is an educational and reflective service; it does not perform, sell, or " +
                        "deliver magical, spiritual, or other services that produce results. See “No promise or guarantee of " +
                        "results” below.",
                    "Source citations and editorial notes are provided where available. Historical materials can be " +
                        "incomplete, contested, symbolic, or difficult to translate.",
                ),
            ),
            LegalSection(
                "No promise or guarantee of results",
                listOf(
                    "Jaime Sorrow does not promise, represent, warrant, or guarantee that any spell, potion, charm, ritual, " +
                        "ceremony, invocation, incantation, divination, meditation, or other magical, spiritual, religious, or " +
                        "occult practice described in or suggested by the app (each, a “Practice”) will produce any result, " +
                        "effect, or outcome of any kind, whether it is performed exactly as described, adapted, repeated, or " +
                        "combined with any other Practice.",
                    "Without limiting the paragraph above, Wiccan makes no promise of any supernatural, paranormal, magical, " +
                        "psychic, or spiritual effect; any physical, medical, psychological, or emotional effect; any change in " +
                        "luck, fortune, money, employment, love, relationships, fertility, protection, or legal matters; or any " +
                        "effect on another person, animal, property, place, or event.",
                    "Practices are provided solely as historical, cultural, literary, and educational material and as prompts " +
                        "for personal reflection. Wiccan does not claim that any Practice is effective, and no scientific " +
                        "evidence establishes that one is. Where the app describes a Practice's purpose or the effect a source or " +
                        "tradition attributed to it, that description reports a historical or traditional claim; it is not a " +
                        "statement of fact, prediction, or promise by Jaime Sorrow.",
                    "Any meaning or outcome you attribute to a Practice is your own interpretation. Jaime Sorrow is not " +
                        "responsible for any outcome, or absence of an outcome, from performing, attempting, or relying on a " +
                        "Practice. The absence of any result is not a defect in, or failure of, the service; your subscription " +
                        "pays for access to educational content, not for any result.",
                    "Do not rely on any Practice in place of professional advice, medical or mental-health care, legal " +
                        "process, financial planning, or emergency services, or use a Practice to make decisions about your " +
                        "health, safety, money, relationships, or legal rights.",
                ),
            ),
            LegalSection(
                "Eligibility and your account",
                listOf(
                    "You must be at least 18 years old, or the age of majority where you live if that is higher, to create an " +
                        "account or use Wiccan. By accepting these Terms, you confirm that you meet this age requirement and are " +
                        "legally able to enter this agreement where you live.",
                    "Keep your sign-in credentials secure, provide accurate account information, and tell support promptly if " +
                        "you believe your account was misused.",
                ),
            ),
            LegalSection(
                "Safety and user responsibility",
                listOf(
                    "Use all content as reflection or historical education, not as medical, mental-health, legal, financial, " +
                        "or emergency advice. Contact an appropriate qualified professional for those needs and local emergency " +
                        "services in an emergency.",
                    "Do not ingest, inhale, apply, or burn unknown or hazardous substances. Follow fire safety, ventilation, " +
                        "allergy, poison-control, property, wildlife, and local-law requirements. Obtain consent before involving " +
                        "another person or their property. You are responsible for deciding whether an activity is safe and " +
                        "lawful.",
                    "The Content & Safety guidance forms part of these Terms and should be reviewed before attempting any " +
                        "optional modern practice.",
                ),
            ),
            LegalSection(
                "Subscriptions and billing",
                listOf(
                    "Wiccan requires a Google Play subscription after sign-in and onboarding. There is no free content plan. " +
                        "Eligible new subscribers receive a 7-day free trial, followed by an automatically renewing monthly " +
                        "subscription with a US base price of \$5.99. The purchase screen and Google Play show your actual " +
                        "eligibility, localized price, and billing terms before you confirm.",
                    "If a trial is offered, cancel before its end to avoid the first monthly charge. If no trial is " +
                        "available, Google Play charges when you confirm. An active trial or subscription is required for all " +
                        "daily content and Grimoire access.",
                    "Unless canceled before renewal, the subscription renews at the then-current price and period shown by " +
                        "Google Play. Manage or cancel it through Google Play → Payments & subscriptions → Subscriptions. " +
                        "Cancellation normally preserves access until the verified expiry date. When access expires, the app " +
                        "returns to the subscription screen; delivery history is preserved for renewal. Refund eligibility is " +
                        "governed by applicable law and Google Play's refund rules.",
                    "Restoring a purchase verifies entitlement with Google Play. Deleting an account first attempts to cancel " +
                        "active auto-renewal; deletion does not itself promise a refund.",
                ),
            ),
            LegalSection(
                "Acceptable use",
                listOf(
                    "Do not misuse the service, interfere with its security, attempt unauthorized access, reverse engineer " +
                        "protected services except where law permits, or use the app to harm, threaten, deceive, or violate " +
                        "another person's rights.",
                ),
            ),
            LegalSection(
                "Content, sources, and AI assistance",
                listOf(
                    "Some editorial drafts may be created with AI assistance from vetted ancient-grimoire source records. " +
                        "Drafts require human source, duplication, safety, and rights review before publication. Wiccan remains " +
                        "responsible for the content it publishes.",
                    "The app and its original presentation are licensed for personal, non-commercial use. Source works and " +
                        "third-party material remain subject to their own rights.",
                ),
            ),
            LegalSection(
                "Availability, suspension, and changes",
                listOf(
                    "The service may change, experience interruptions, or remove content for safety, rights, accuracy, or " +
                        "operational reasons. Access may be suspended for material breach, misuse, legal requirements, or " +
                        "security risk.",
                    "Material Terms changes receive a new version and require acceptance in the app before you can continue " +
                        "using it.",
                ),
            ),
            LegalSection(
                "Disclaimers and limitation of liability",
                listOf(
                    "Wiccan is provided “as is” and “as available.” To the fullest extent permitted by law, Jaime Sorrow " +
                        "disclaims all warranties, express or implied, including merchantability, fitness for a particular " +
                        "purpose, and non-infringement, and does not promise that the service will be uninterrupted, error-free, " +
                        "or that any content is complete or accurate.",
                    "To the fullest extent permitted by law, Jaime Sorrow is not liable for indirect, incidental, special, " +
                        "consequential, or punitive damages, or for any injury, loss, or damage arising from your decision to " +
                        "attempt a Practice or from any result, or absence of a result, of a Practice. Our total liability for " +
                        "any claim relating to the service is limited to the greater of the amount you paid for Wiccan in the 12 " +
                        "months before the claim arose or US \$50.",
                    "Some places do not allow certain disclaimers or limitations, so parts of this section may not apply to " +
                        "you. Nothing in these Terms limits rights you have under applicable consumer-protection law.",
                ),
            ),
            LegalSection(
                "Governing law and disputes",
                listOf(
                    "These Terms are governed by the laws of the State of Alaska, United States, and applicable U.S. federal " +
                        "law, without regard to conflict-of-law rules. The state and federal courts located in Alaska have " +
                        "jurisdiction over disputes relating to these Terms or the service.",
                    "If you are a consumer living outside Alaska, including outside the United States, you keep the " +
                        "protection of the mandatory consumer laws of the place where you live, and you may also bring a claim in " +
                        "the courts where you live if those laws allow it. Before starting a formal dispute, please email " +
                        "wiccan.android.app@gmail.com so we can try to resolve it informally.",
                ),
            ),
            LegalSection(
                "Contact and account deletion",
                listOf(
                    "Wiccan is operated by Jaime Sorrow. For questions, email wiccan.android.app@gmail.com or use Contact " +
                        "Support in Settings.",
                    "You may delete your account in Settings → Delete Account, or request deletion without the app at " +
                        "https://wiccan-android.web.app/delete-account.",
                ),
            ),
        ),
    )

    val privacy = LegalDocument(
        id = LegalDocumentId.PRIVACY,
        title = "Privacy Policy",
        version = PolicyVersions.PRIVACY,
        effectiveDate = "November 7, 2026",
        introduction =
            "This Privacy Policy explains what information Jaime Sorrow (“Wiccan,” “we,” or “us”) handles when you " +
                "use the Wiccan Android app, why it is used, and the choices available to you. Wiccan does not sell " +
                "personal data and does not show advertising.",
        sections = listOf(
            LegalSection(
                "Information handled",
                listOf(
                    "Account data: email address, first and last name, Firebase user ID, sign-in provider, and " +
                        "policy-acceptance versions and timestamps.",
                    "Service activity: delivered content IDs, dates, source metadata, daily-use reservations, time zone, " +
                        "notification preference, and registered push token.",
                    "Purchase data: product ID, entitlement and renewal state, expiry, purchase token, and pseudonymous " +
                        "account/token hashes used to prevent purchase fraud and replay.",
                    "Security signals: App Check and Play Integrity attestations, used on every request to protect the " +
                        "service from fraud and abuse.",
                    "Optional analytics and crash reports: app interactions and device/app information through Firebase " +
                        "Analytics, and crash reports through Crashlytics, are collected only if you opt in during onboarding or " +
                        "in Settings, and you can turn them off at any time. While analytics is on and your device allows it, " +
                        "Firebase Analytics may also collect the Android advertising ID; Wiccan uses it only for analytics " +
                        "measurement, never for advertising, and tells Google that ad storage, ad user data, and ad " +
                        "personalization are denied. You can reset or delete the advertising ID in your device's Google or " +
                        "privacy settings.",
                    "The app does not request precise location, contacts, photos, camera, microphone, or health permissions. " +
                        "A device time-zone identifier is not used to infer precise location.",
                ),
            ),
            LegalSection(
                "Why information is used",
                listOf(
                    "Information is used to authenticate accounts, avoid repeating daily content, show archive history, " +
                        "deliver requested notifications, verify subscription access, prevent fraud, enforce security, meet legal " +
                        "or platform obligations, and, only if you opt in, diagnose crashes and understand app performance.",
                ),
            ),
            LegalSection(
                "Legal bases (EEA, UK, Switzerland, and similar laws)",
                listOf(
                    "Where the GDPR, UK GDPR, Swiss law, or a similar law applies, Wiccan relies on these legal bases: " +
                        "performance of its contract with you, to create and secure your account, deliver the daily content and " +
                        "notifications you request, and provide your subscription; legitimate interests, to prevent fraud and " +
                        "abuse and keep the service secure, balanced against your rights; your consent, for usage analytics and " +
                        "crash reports, which you can withdraw at any time in Settings; and legal obligations, such as tax and " +
                        "accounting records and responding to lawful requests.",
                    "You may object at any time to processing based on legitimate interests by emailing " +
                        "wiccan.android.app@gmail.com.",
                ),
            ),
            LegalSection(
                "Processors and disclosure",
                listOf(
                    "Google services process data on Wiccan's behalf or under their own terms: Firebase Authentication, " +
                        "Firestore, Cloud Functions, Cloud Messaging, Analytics, Crashlytics, App Check, Play Integrity, and " +
                        "Google Play Billing.",
                    "Information may also be disclosed when required by law, to protect users or the service, or during a " +
                        "lawful business transfer. Wiccan does not sell personal data or share it for third-party behavioral " +
                        "advertising.",
                ),
            ),
            LegalSection(
                "Retention and deletion",
                listOf(
                    "Account and service data is kept while your account exists and is needed for the purposes above. " +
                        "Event-level Firebase Analytics data is kept for no longer than 14 months, and Crashlytics crash reports " +
                        "for about 90 days.",
                    "Delete Account in Settings first attempts to cancel active Play auto-renewal, then deletes the Firebase " +
                        "account, profile, delivery history, usage reservations, registered devices, and subscription record. If " +
                        "cancellation cannot be confirmed, deletion stops so it can be retried safely.",
                    "Limited pseudonymous purchase-token and account hashes may remain for fraud and anti-replay protection, " +
                        "for as long as needed to stop a purchase being reused on another account. They are not the raw purchase " +
                        "token or an active account profile.",
                    "You may also request deletion without the app at https://wiccan-android.web.app/delete-account. " +
                        "Legitimate legal, security, dispute, or fraud records may be retained only as necessary.",
                ),
            ),
            LegalSection(
                "Security and international processing",
                listOf(
                    "Wiccan uses transport encryption, Firebase security rules, server-authoritative writes, App Check, Play " +
                        "Integrity, and restricted administrator workflows. No security method can guarantee absolute protection.",
                    "Wiccan is operated from the United States, and Google services may process information in the United " +
                        "States and other countries outside your own. Transfers of personal data from the EEA, UK, or Switzerland " +
                        "rely on the safeguards in Google's data processing terms, such as the EU-U.S. Data Privacy Framework and " +
                        "Standard Contractual Clauses.",
                ),
            ),
            LegalSection(
                "Children",
                listOf(
                    "Wiccan is intended only for adults (18 or older, or the age of majority where they live if higher) and " +
                        "is not directed to children. We do not knowingly collect personal information from anyone under 18. If " +
                        "you believe a minor has created an account, email wiccan.android.app@gmail.com and we will delete it.",
                ),
            ),
            LegalSection(
                "Your privacy rights",
                listOf(
                    "Depending on where you live, you may have rights over your personal information. In the European " +
                        "Economic Area, the United Kingdom, and Switzerland, these include access, correction, deletion, " +
                        "restriction, objection to processing based on legitimate interests, data portability, and withdrawing " +
                        "any consent you gave. Residents of California and other U.S. states with privacy laws, Canada, Brazil, " +
                        "Australia, and other countries may have similar rights, including the right to know what is collected " +
                        "and to opt out of its sale, sharing for targeted advertising, or profiling.",
                    "Wiccan does not sell personal information, share it for cross-context behavioral advertising, or use it " +
                        "for profiling with legal or similarly significant effects.",
                    "To make a request, email wiccan.android.app@gmail.com from the address on your account. We will verify " +
                        "the request against your account and respond within 30 days. If we decline a request, you may appeal by " +
                        "replying to our response. We will not discriminate against you for exercising these rights. If you are " +
                        "in the EEA, UK, or Switzerland, you may also complain to your local data-protection authority.",
                ),
            ),
            LegalSection(
                "Your choices and contact",
                listOf(
                    "In Settings you can disable daily notifications, turn usage analytics and crash reports on or off, sign " +
                        "out, and delete your account. Manage your subscription in Google Play.",
                    "Wiccan is operated by Jaime Sorrow, an individual developer based in Alaska, United States, who is " +
                        "responsible for (the “controller” of) your personal information. For access, correction, privacy, or " +
                        "deletion questions, email wiccan.android.app@gmail.com or use Contact Support in Settings.",
                    "When this policy changes materially, it receives a new version and the app asks you to review it before " +
                        "continuing.",
                ),
            ),
        ),
    )

    val contentSafety = LegalDocument(
        id = LegalDocumentId.CONTENT_SAFETY,
        title = "Content & Safety",
        version = null,
        effectiveDate = "November 7, 2026",
        introduction =
            "Wiccan treats historical spiritual material as education and reflection. This guidance applies to every " +
                "lesson, spell, potion, and optional practice in the app.",
        sections = listOf(
            LegalSection(
                "No promised outcomes",
                listOf(
                    "Wiccan does not promise or guarantee that any spell, potion, ritual, ceremony, or other practice will " +
                        "produce any supernatural, physical, emotional, financial, relational, or other result. Practices are " +
                        "presented as history and as prompts for reflection; descriptions of what a practice was meant to do " +
                        "report historical or traditional claims, not facts. The Terms of Service contain the full disclaimer.",
                ),
            ),
            LegalSection(
                "No professional or emergency advice",
                listOf(
                    "Content is not medical, mental-health, legal, financial, veterinary, or emergency advice. It is not a " +
                        "diagnosis or treatment. Seek an appropriate qualified professional, and contact local emergency services " +
                        "or poison control when needed.",
                ),
            ),
            LegalSection(
                "Materials, fire, and physical safety",
                listOf(
                    "Never ingest, inhale, apply, forage, mix, or burn a substance unless you have independently confirmed " +
                        "that it is safe for the intended use. Avoid unknown plants, toxic minerals, bodily fluids, unsafe smoke, " +
                        "and hazardous reactions.",
                    "Use flame-safe surfaces, ventilation, supervision, and local fire rules. Keep materials away from " +
                        "children and animals. Substitute a written or visual reflection whenever physical practice creates risk.",
                ),
            ),
            LegalSection(
                "Consent, law, and respect",
                listOf(
                    "Do not involve another person, their property, remains, protected sites, wildlife, or restricted " +
                        "cultural material without permission and lawful authority. Respect living traditions and do not present " +
                        "a modern adaptation as an authentic reconstruction.",
                ),
            ),
            LegalSection(
                "Editorial standard",
                listOf(
                    "AI-assisted drafts can only use vetted ancient-grimoire source records. A human must review source " +
                        "grounding, duplication, safety, rights, and wording before publication.",
                    "Report a safety, rights, or accuracy concern through Contact Support. Content may be corrected or " +
                        "withdrawn while it is reviewed.",
                ),
            ),
        ),
    )
}
