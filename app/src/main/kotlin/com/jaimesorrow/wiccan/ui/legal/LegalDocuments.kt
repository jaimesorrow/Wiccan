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
 * policy acceptance, subscription purchase, and from Settings. The public
 * hosted policies remain release requirements; see PolicyUrls and the Play
 * compliance audit.
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
        effectiveDate = "October 21, 2026",
        introduction =
            "These Terms govern your use of Wiccan. By accepting them, you agree to use " +
                "the app responsibly and in accordance with applicable law.",
        sections = listOf(
            LegalSection(
                "What Wiccan provides",
                listOf(
                    "Wiccan presents educational, historical, and spiritual-reflection content " +
                        "inspired by ancient grimoires and related primary sources. It does not " +
                        "guarantee supernatural, health, financial, legal, or other outcomes.",
                    "Source citations and editorial notes are provided where available. Historical " +
                        "materials can be incomplete, contested, symbolic, or difficult to translate.",
                ),
            ),
            LegalSection(
                "Eligibility and your account",
                listOf(
                    "You must be legally able to enter this agreement where you live. If you are " +
                        "not old enough to do so independently, a parent or legal guardian must " +
                        "review and accept these Terms for you.",
                    "Keep your sign-in credentials secure, provide accurate account information, " +
                        "and tell support promptly if you believe your account was misused.",
                ),
            ),
            LegalSection(
                "Safety and user responsibility",
                listOf(
                    "Use all content as reflection or historical education, not as medical, mental-" +
                        "health, legal, financial, or emergency advice. Contact an appropriate " +
                        "qualified professional for those needs and local emergency services in an emergency.",
                    "Do not ingest, inhale, apply, or burn unknown or hazardous substances. Follow " +
                        "fire safety, ventilation, allergy, poison-control, property, wildlife, and " +
                        "local-law requirements. Obtain consent before involving another person or " +
                        "their property. You are responsible for deciding whether an activity is safe and lawful.",
                    "The Content & Safety screen forms part of this guidance and should be reviewed " +
                        "before attempting any optional modern practice.",
                ),
            ),
            LegalSection(
                "Subscriptions and billing",
                listOf(
                    "Wiccan requires a Google Play subscription after sign-in and onboarding. There is no " +
                        "free content plan. Eligible new subscribers receive a 7-day free trial, followed by " +
                        "an automatically renewing monthly subscription with a US base price of $5.99. " +
                        "The purchase screen and Google Play show your actual eligibility, localized price, " +
                        "and billing terms before you confirm.",
                    "If a trial is offered, cancel before its end to avoid the first monthly charge. " +
                        "If no trial is available, Google Play charges when you confirm. An active trial " +
                        "or subscription is required for all daily content and Grimoire access.",
                    "Unless canceled before renewal, the subscription renews at the then-current " +
                        "price and period shown by Google Play. Manage or cancel it through Google " +
                        "Play → Payments & subscriptions → Subscriptions. Cancellation normally preserves " +
                        "access until the verified expiry date. When access expires, the app returns to the " +
                        "subscription screen; delivery history is preserved for renewal. " +
                        "Refund eligibility is governed by applicable law and Google Play's refund rules.",
                    "Restoring a purchase verifies entitlement with Google Play. Deleting an account " +
                        "first attempts to cancel active auto-renewal; deletion does not itself promise a refund.",
                ),
            ),
            LegalSection(
                "Acceptable use",
                listOf(
                    "Do not misuse the service, interfere with its security, attempt unauthorized " +
                        "access, reverse engineer protected services except where law permits, or use " +
                        "the app to harm, threaten, deceive, or violate another person's rights.",
                ),
            ),
            LegalSection(
                "Content, sources, and AI assistance",
                listOf(
                    "Some editorial drafts may be created with AI assistance from vetted ancient-" +
                        "grimoire source records. Drafts require human source, duplication, safety, " +
                        "and rights review before publication. Wiccan remains responsible for the " +
                        "content it publishes.",
                    "The app and its original presentation are licensed for personal, non-commercial " +
                        "use. Source works and third-party material remain subject to their own rights.",
                ),
            ),
            LegalSection(
                "Availability, suspension, and changes",
                listOf(
                    "The service may change, experience interruptions, or remove content for safety, " +
                        "rights, accuracy, or operational reasons. Access may be suspended for material " +
                        "breach, misuse, legal requirements, or security risk.",
                    "Material Terms changes receive a new version and require acceptance in the app. " +
                        "Applicable consumer rights are not limited by these Terms.",
                ),
            ),
            LegalSection(
                "Contact and account deletion",
                listOf(
                    "Use Contact Support in Settings for questions. You may delete your account from " +
                        "Settings. Privacy or deletion questions can be sent through the same support path.",
                ),
            ),
        ),
    )

    val privacy = LegalDocument(
        id = LegalDocumentId.PRIVACY,
        title = "Privacy Policy",
        version = PolicyVersions.PRIVACY,
        effectiveDate = "October 12, 2026",
        introduction =
            "This notice explains the information Wiccan handles, why it is used, and the choices " +
                "available to you. Wiccan does not sell personal data and does not include advertising.",
        sections = listOf(
            LegalSection(
                "Information handled",
                listOf(
                    "Account data: email address, Firebase user ID, sign-in provider, and policy-" +
                        "acceptance versions and timestamps.",
                    "Service activity: delivered content IDs, dates, source metadata, daily-use " +
                        "reservations, time zone, notification preference, and registered push token.",
                    "Purchase data: product ID, entitlement and renewal state, expiry, purchase token, " +
                        "and pseudonymous account/token hashes used to prevent purchase fraud and replay.",
                    "Diagnostics and analytics: app interactions, device/app information, crash reports, " +
                        "and integrity signals handled through Firebase Analytics, Crashlytics, App Check, " +
                        "and Play Integrity.",
                    "The app does not request precise location, contacts, photos, camera, microphone, " +
                        "or health permissions. A device time-zone identifier is not used to infer precise location.",
                ),
            ),
            LegalSection(
                "Why information is used",
                listOf(
                    "Information is used to authenticate accounts, avoid repeating daily content, " +
                        "show archive history, deliver requested notifications, verify subscription access, " +
                        "prevent fraud, enforce security, diagnose failures, understand app performance, " +
                        "and meet legal or platform obligations.",
                ),
            ),
            LegalSection(
                "Processors and disclosure",
                listOf(
                    "Google services process data on Wiccan's behalf or under their own terms: Firebase " +
                        "Authentication, Firestore, Cloud Functions, Cloud Messaging, Analytics, " +
                        "Crashlytics, App Check, Play Integrity, and Google Play Billing.",
                    "Information may also be disclosed when required by law, to protect users or the " +
                        "service, or during a lawful business transfer. Wiccan does not sell personal " +
                        "data or share it for third-party behavioral advertising.",
                ),
            ),
            LegalSection(
                "Retention and deletion",
                listOf(
                    "Account and service data is kept while needed to operate the account and for the " +
                        "purposes above. Analytics and crash retention follow the configured Firebase " +
                        "controls and must be verified by the operator before release.",
                    "Delete Account in Settings first attempts to cancel active Play auto-renewal, then " +
                        "deletes the Firebase account, profile, delivery history, usage reservations, " +
                        "registered devices, and subscription record. If cancellation cannot be " +
                        "confirmed, deletion stops so it can be retried safely.",
                    "Limited pseudonymous purchase-token and account hashes may remain for fraud and " +
                        "anti-replay protection. They are not the raw purchase token or an active account profile.",
                    "You may also submit a deletion request through the public account-deletion page. " +
                        "Legitimate legal, security, dispute, or fraud records may be retained only as necessary.",
                ),
            ),
            LegalSection(
                "Security and international processing",
                listOf(
                    "Wiccan uses transport encryption, Firebase security rules, server-authoritative " +
                        "writes, App Check, Play Integrity, and restricted administrator workflows. No " +
                        "security method can guarantee absolute protection.",
                    "Google services may process information in countries outside your own, subject to " +
                        "their safeguards and applicable transfer requirements.",
                ),
            ),
            LegalSection(
                "Your choices and contact",
                listOf(
                    "You can disable daily notifications, sign out, manage your subscription in Google " +
                        "Play, and delete your account in Settings. Contact Support for access, correction, " +
                        "privacy, or deletion questions. Applicable regional privacy rights remain available.",
                ),
            ),
        ),
    )

    val contentSafety = LegalDocument(
        id = LegalDocumentId.CONTENT_SAFETY,
        title = "Content & Safety",
        version = null,
        effectiveDate = "October 12, 2026",
        introduction =
            "Wiccan treats historical spiritual material as education and reflection. This guidance " +
                "applies to every lesson, spell, potion, and optional practice in the app.",
        sections = listOf(
            LegalSection(
                "No professional or emergency advice",
                listOf(
                    "Content is not medical, mental-health, legal, financial, veterinary, or emergency " +
                        "advice. It is not a diagnosis or treatment. Seek an appropriate qualified " +
                        "professional, and contact local emergency services or poison control when needed.",
                ),
            ),
            LegalSection(
                "Materials, fire, and physical safety",
                listOf(
                    "Never ingest, inhale, apply, forage, mix, or burn a substance unless you have " +
                        "independently confirmed that it is safe for the intended use. Avoid unknown " +
                        "plants, toxic minerals, bodily fluids, unsafe smoke, and hazardous reactions.",
                    "Use flame-safe surfaces, ventilation, supervision, and local fire rules. Keep " +
                        "materials away from children and animals. Substitute a written or visual " +
                        "reflection whenever physical practice creates risk.",
                ),
            ),
            LegalSection(
                "Consent, law, and respect",
                listOf(
                    "Do not involve another person, their property, remains, protected sites, wildlife, " +
                        "or restricted cultural material without permission and lawful authority. Respect " +
                        "living traditions and do not present a modern adaptation as an authentic reconstruction.",
                ),
            ),
            LegalSection(
                "Editorial standard",
                listOf(
                    "AI-assisted drafts can only use vetted ancient-grimoire source records. A human " +
                        "must review source grounding, duplication, safety, rights, and wording before publication.",
                    "Report a safety, rights, or accuracy concern through Contact Support. Content may " +
                        "be corrected or withdrawn while it is reviewed.",
                ),
            ),
        ),
    )
}
