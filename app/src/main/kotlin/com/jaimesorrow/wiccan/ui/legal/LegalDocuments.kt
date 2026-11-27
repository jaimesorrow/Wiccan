package com.jaimesorrow.wiccan.ui.legal

import com.jaimesorrow.wiccan.domain.model.PolicyVersions

enum class LegalDocumentId {
    TERMS,
    PRIVACY,
    CONTENT_SAFETY,
    POTION_SAFETY,
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
        LegalDocumentId.POTION_SAFETY -> potionSafety
    }

    val terms = LegalDocument(
        id = LegalDocumentId.TERMS,
        title = "Terms of Service",
        version = PolicyVersions.TERMS,
        effectiveDate = "November 21, 2026",
        introduction =
            "These Terms of Service (“Terms”) are an agreement between you and Jaime Sorrow (“Wiccan,” “we,” or “us”) " +
                "and govern your use of the Wiccan app and related services. By accepting them, you agree to use the app " +
                "responsibly and in accordance with applicable law.",
        sections = listOf(
            LegalSection(
                "What Wiccan provides",
                listOf(
                    "Wiccan presents educational, historical, and spiritual-reflection content: practical lessons, spells, and " +
                        "potions drawn from historical grimoires and ancient magical texts, each cited where available. It " +
                        "is an educational and reflective service; it does not perform, sell, or deliver magical, spiritual, or " +
                        "other services that produce results. See “No promise or guarantee of results” below.",
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
                    "Wiccan requires a Google Play subscription after sign-in and onboarding. There is no free content " +
                        "plan. Wiccan Premium comes in two plans: a monthly plan with a US base price of \$5.99 per month, " +
                        "and a yearly plan with a US base price of \$39.99 per year. Eligible new subscribers receive a free " +
                        "trial at the length shown at checkout (7 days on the monthly plan, 14 days on the yearly plan), " +
                        "followed by the automatically renewing plan they chose. The purchase screen and Google " +
                        "Play show your actual eligibility, localized price, billing period, and terms before you confirm.",
                    "Monthly plan: after any free trial, Google Play charges the monthly price once a month until you " +
                        "cancel. Yearly plan: after any free trial, Google Play charges the yearly price once a year until " +
                        "you cancel. If a trial is offered, cancel before it ends to avoid the first charge for your plan. If " +
                        "no trial is available, Google Play charges when you confirm. During a free trial, the app may send a " +
                        "reminder before the trial ends. An active trial or subscription is required for all daily content " +
                        "and Grimoire access.",
                    "Unless canceled before renewal, either plan renews at the then-current price and period shown by " +
                        "Google Play. You can cancel either plan at any time through Google Play → Payments & subscriptions → " +
                        "Subscriptions. Cancellation normally preserves access until the verified expiry date. When access " +
                        "expires, the app returns to the subscription screen; delivery history is preserved for renewal. " +
                        "Refund eligibility is governed by applicable law and Google Play's refund rules.",
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
                    "Some editorial drafts may be created with AI assistance from historical grimoires and ancient magical " +
                        "texts. Drafts are published after an automated and AI review under the owner's authorization, which " +
                        "checks that each lesson is a full step-by-step practice correctly cited to those sources, and includes " +
                        "safety, rights, and originality checks. Wiccan remains responsible for the content it publishes.",
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
                        "hello@wiccan.app so we can try to resolve it informally.",
                ),
            ),
            LegalSection(
                "Contact and account deletion",
                listOf(
                    "Wiccan is operated by Jaime Sorrow. For questions, email hello@wiccan.app or use Contact " +
                        "Support in Settings.",
                    "You may delete your account in Settings → Delete Account, or request deletion without the app at " +
                        "https://wiccan.app/delete-account.html.",
                ),
            ),
        ),
    )

    val privacy = LegalDocument(
        id = LegalDocumentId.PRIVACY,
        title = "Privacy Policy",
        version = PolicyVersions.PRIVACY,
        effectiveDate = "November 26, 2026",
        introduction =
            "This Privacy Policy explains what information Jaime Sorrow (“Wiccan,” “we,” or “us”) handles when " +
                "you use the Wiccan Android app and its related services, why it is used, which service providers " +
                "process it, how long it is kept, and the choices available to you. Wiccan does not sell personal " +
                "data. If you do not have an active subscription, the app shows ads from Google AdMob; subscribers " +
                "(monthly or yearly, including during a free trial) see no ads.",
        sections = listOf(
            LegalSection(
                "Who we are",
                listOf(
                    "Wiccan is developed and operated by Jaime Sorrow, an individual developer based in Alaska, United States, " +
                        "who is responsible for (the “controller” of) your personal information. For any privacy question or " +
                        "request, email hello@wiccan.app or use Contact Support in Settings.",
                ),
            ),
            LegalSection(
                "Information handled",
                listOf(
                    "Account data: email address, first and last name, Firebase user ID, sign-in provider, email-verification " +
                        "status, and policy-acceptance versions and timestamps. If you sign up with an email address, your password " +
                        "is stored only in hashed form by Firebase Authentication and is never visible to Wiccan. If you sign in " +
                        "with Google, Google shares your Google account's name, email address, and profile-picture link with " +
                        "Firebase Authentication; Wiccan does not display or otherwise use the profile picture.",
                    "Service activity: delivered content IDs, dates, source metadata, daily-use reservations, records of when " +
                        "the content library runs low for your account, time zone, notification preference, registered push token, " +
                        "and the time of your most recent verification-email request (used to limit resends).",
                    "Purchase data: product ID, entitlement and renewal state, expiry, purchase token, and pseudonymous " +
                        "account/token hashes used to prevent purchase fraud and replay. Google Play processes your payment; Wiccan " +
                        "never receives your card or other payment details, and it gives Google Play only a one-way hashed account " +
                        "identifier, never your raw user ID.",
                    "Security signals: App Check and Play Integrity attestations, used on every request to protect the service " +
                        "from fraud and abuse. When the app connects, Google's servers also receive your device's IP address, which " +
                        "Firebase Authentication and Cloud Functions use to process requests and protect sign-in from abuse. If a " +
                        "request fails, the server records your user ID and the error in Google Cloud Logging so the problem can be " +
                        "investigated.",
                    "Optional analytics and crash reports: app interactions and device/app information through Firebase " +
                        "Analytics, and crash reports through Crashlytics, are collected only if you opt in during onboarding or in " +
                        "Settings, and you can turn them off at any time. App interactions include events such as completing " +
                        "sign-up, viewing the day's content (with its type and tier), viewing the subscription screen, starting or " +
                        "losing a subscription, and deleting an account. Device/app information includes the device model, " +
                        "operating-system and app versions, and Firebase installation identifiers. Firebase Analytics also records " +
                        "screen views, sessions, and Google Play subscription events (product and price), and derives an " +
                        "approximate location, such as country or city, from a masked IP address. Crash reports include the stack " +
                        "trace and the app's state at the time of the crash. While analytics is on and your device allows it, " +
                        "Firebase Analytics may also collect the Android advertising ID; Wiccan uses it only for analytics " +
                        "measurement, never for advertising, and tells Google that ad storage, ad user data, and ad personalization " +
                        "are denied. You can reset or delete the advertising ID in your device's Google or privacy settings.",
                    "Ads for non-subscribers (Google AdMob): if you do not have an active subscription, the Grimoire " +
                        "screen shows one banner ad from Google AdMob. No ads appear in lessons or rituals, on the " +
                        "subscription screen, in Settings, during account deletion, on legal pages, or during email " +
                        "verification, and subscribers see no ads. To choose, show, and measure these ads and to prevent ad " +
                        "fraud, the Google Mobile Ads SDK and Google's consent tool may collect your Android advertising ID, " +
                        "device and app information (such as device model, operating-system and app versions, language, and " +
                        "screen size), ad interactions (such as ads shown and tapped), diagnostic and performance " +
                        "information, your consent choices, and your IP address, from which Google may derive an approximate " +
                        "location such as country or city. Where the law requires consent, such as in the European Economic " +
                        "Area, the United Kingdom, and Switzerland, Google's consent message asks for your choices before any " +
                        "ad is requested; where U.S. state privacy laws apply, it lets you opt out of personalized ads. If " +
                        "you do not allow personalized ads, Google may still show non-personalized ads, which use the context " +
                        "of the app and limited data for frequency capping, fraud prevention, and measurement. You can change " +
                        "your choice at any time with Privacy choices in Settings, where offered, and you can reset or delete " +
                        "the advertising ID in your device's Google or privacy settings. Wiccan does not send your name, " +
                        "email address, or account ID to AdMob.",
                    "On your device: the app keeps today's content in a local cache and stores your theme, notification, and " +
                        "analytics choices. The content cache and sign-in data are excluded from Android backups; your theme, " +
                        "notification, and analytics choices may be included in your device's own Android backup. Uninstalling the " +
                        "app removes this data from the device but does not delete your account.",
                    "Support messages: if you email hello@wiccan.app, we receive your email address and whatever your message " +
                        "contains.",
                    "The app requests only the notification permission, for the optional daily reminder and, during a " +
                        "free trial, a reminder before the trial ends. You can turn either off in Android's notification " +
                        "settings for Wiccan. It does not request precise location, contacts, photos, camera, microphone, or " +
                        "health permissions. A device time-zone identifier is not used to infer precise location. The app " +
                        "also declares Android's advertising ID permission, which shows no prompt, so the Google Mobile Ads " +
                        "SDK can read the advertising ID described above.",
                ),
            ),
            LegalSection(
                "Why information is used",
                listOf(
                    "Information is used to authenticate accounts, send account emails (verification, password reset, and " +
                        "email-change confirmation), avoid repeating daily content, show archive history, deliver requested " +
                        "notifications, remind you before a free trial ends, verify subscription access, answer support " +
                        "requests, prevent fraud, enforce security, meet legal or platform obligations, show and measure ads " +
                        "for users without a subscription, and, only if you opt in, diagnose crashes and understand app " +
                        "performance.",
                    "Wiccan uses an AI service (Anthropic's Claude) on its servers only to draft lesson text from historical " +
                        "source passages. Lessons are then checked by automated tools and an AI assistant under the owner's " +
                        "authorization before publishing. No account, usage, device, or other personal information is sent to the " +
                        "AI provider, and the app has no feature that sends what you type to an AI service.",
                ),
            ),
            LegalSection(
                "Legal bases (EEA, UK, Switzerland, and similar laws)",
                listOf(
                    "Where the GDPR, UK GDPR, Swiss law, or a similar law applies, Wiccan relies on these legal bases: " +
                        "performance of its contract with you, to create and secure your account, deliver the daily content " +
                        "and notifications you request, and provide your subscription; legitimate interests, to prevent fraud " +
                        "and abuse, keep the service secure, and show non-personalized ads to users without a subscription " +
                        "where the law allows, balanced against your rights; your consent, for usage analytics and crash " +
                        "reports, which you can withdraw at any time in Settings, and for personalized ads and storing or " +
                        "reading ad information on your device, which you can change at any time with Privacy choices in " +
                        "Settings, where offered; and legal obligations, such as tax and accounting records and responding to " +
                        "lawful requests.",
                    "You may object at any time to processing based on legitimate interests by emailing hello@wiccan.app.",
                ),
            ),
            LegalSection(
                "Service providers and disclosure",
                listOf(
                    "Wiccan does not share personal data with third parties for their own marketing or other independent " +
                        "purposes, except the ad data Google AdMob handles under its own terms, as described below. It relies " +
                        "on these service providers, which process data on Wiccan's behalf or, where noted, under their own " +
                        "terms:",
                    "Google: Firebase Authentication, Cloud Firestore, Cloud Functions, Cloud Messaging, Firebase " +
                        "Installations, Analytics, Crashlytics, App Check, Google Cloud Logging, and Secret Manager, for sign-in, " +
                        "storage, server processing, notifications, optional diagnostics, and security; and the Gmail mailbox that " +
                        "receives hello@wiccan.app support email.",
                    "Google Play: subscription purchases, billing, refunds, and Play Integrity checks, under Google's own terms " +
                        "and privacy policy.",
                    "Google AdMob: ads shown to users without a subscription, together with Google's consent message, " +
                        "under Google's own terms and privacy policy. Google acts independently for some of this processing, " +
                        "as explained at https://policies.google.com/technologies/partner-sites.",
                    "Cloudflare Email Routing: handles mail to hello@wiccan.app.",
                    "Firebase Authentication: sends the verification, password-reset, and change-email messages.",
                    "Information may also be disclosed when required by law, to protect users or the service, or during a " +
                        "lawful business transfer. Wiccan does not sell personal data for money. If you allow personalized " +
                        "ads, Google AdMob may use your advertising ID and other ad data to show ads based on your interests; " +
                        "some U.S. state laws treat this as “sharing” for targeted advertising, and you can opt out with " +
                        "Privacy choices in Settings, where offered or by deleting your advertising ID.",
                ),
            ),
            LegalSection(
                "Retention and deletion",
                listOf(
                    "Account and service data is kept while your account exists and is needed for the purposes above. " +
                        "Event-level Firebase Analytics data is kept for no longer than 14 months, Crashlytics crash reports " +
                        "for about 90 days, and server error logs for about 30 days. Support emails are kept for as long as " +
                        "needed to handle the request and keep a record of it. Ad data collected by Google AdMob is kept by " +
                        "Google under its own retention policies (https://policies.google.com/technologies/retention).",
                    "Delete Account in Settings first attempts to cancel active Play auto-renewal, then deletes the Firebase " +
                        "account, profile, delivery history, usage reservations, content-availability records, registered devices, " +
                        "verification-email record, and subscription record. If cancellation cannot be confirmed, deletion stops so " +
                        "it can be retried safely.",
                    "Limited pseudonymous purchase-token and account hashes may remain for fraud and anti-replay protection, " +
                        "for as long as needed to stop a purchase being reused on another account. They are not the raw purchase " +
                        "token or an active account profile.",
                    "You may also request deletion without the app at https://wiccan.app/delete-account.html; we verify the " +
                        "request and confirm deletion within 30 days. Deleted data may remain in Google's encrypted backups for a " +
                        "limited period until it is overwritten. Legitimate legal, security, dispute, or fraud records may be " +
                        "retained only as necessary.",
                ),
            ),
            LegalSection(
                "Security and international processing",
                listOf(
                    "Wiccan uses transport encryption (HTTPS/TLS) for all traffic, Google Cloud's encryption at rest, Firebase " +
                        "security rules that block the app from writing account, purchase, or delivery records directly, " +
                        "server-authoritative writes, App Check, Play Integrity, server credentials held in Secret Manager, and " +
                        "restricted administrator workflows. No security method can guarantee absolute protection.",
                    "Wiccan is operated from the United States, and Google services may process information in the United " +
                        "States and other countries outside your own. Transfers of personal data from the EEA, UK, or Switzerland " +
                        "rely on the safeguards in Google's data processing terms, such as the EU-U.S. Data Privacy Framework and " +
                        "Standard Contractual Clauses.",
                ),
            ),
            LegalSection(
                "Children",
                listOf(
                    "Wiccan is intended only for adults (18 or older, or the age of majority where they live if higher) and is " +
                        "not directed to children. We do not knowingly collect personal information from anyone under 18. If you " +
                        "believe a minor has created an account, email hello@wiccan.app and we will delete it.",
                ),
            ),
            LegalSection(
                "Your privacy rights",
                listOf(
                    "Depending on where you live, you may have rights over your personal information. In the European Economic " +
                        "Area, the United Kingdom, and Switzerland, these include access, correction, deletion, restriction, " +
                        "objection to processing based on legitimate interests, data portability, and withdrawing any consent you " +
                        "gave. Residents of California and other U.S. states with privacy laws, Canada, Brazil, Australia, and " +
                        "other countries may have similar rights, including the right to know what is collected and to opt out of " +
                        "its sale, sharing for targeted advertising, or profiling.",
                    "Wiccan does not sell personal information for money or use it for profiling with legal or similarly " +
                        "significant effects. Personalized ads from Google AdMob may count as sharing for cross-context " +
                        "behavioral advertising under some U.S. state laws; you can opt out at any time with Privacy choices " +
                        "in Settings, where offered or by deleting your device's advertising ID, and subscribers see no ads.",
                    "To make a request, email hello@wiccan.app from the address on your account. We will verify the request " +
                        "against your account and respond within 30 days. If we decline a request, you may appeal by replying to " +
                        "our response. We will not discriminate against you for exercising these rights. If you are in the EEA, UK, " +
                        "or Switzerland, you may also complain to your local data-protection authority.",
                ),
            ),
            LegalSection(
                "Your choices and contact",
                listOf(
                    "In Settings you can disable daily notifications, turn usage analytics and crash reports on or off, " +
                        "change your ad privacy choices (where offered), sign out, and delete your account. Manage your " +
                        "subscription in Google Play; an active subscription removes all ads.",
                    "For access, correction, privacy, or deletion questions, email hello@wiccan.app or use Contact Support in " +
                        "Settings. Wiccan is operated by Jaime Sorrow.",
                ),
            ),
            LegalSection(
                "Changes to this policy",
                listOf(
                    "The effective date at the top of this policy shows when it last changed. When this policy changes " +
                        "materially, it receives a new version and the app asks you to review it before continuing.",
                ),
            ),
        ),
    )

    val contentSafety = LegalDocument(
        id = LegalDocumentId.CONTENT_SAFETY,
        title = "Content & Safety",
        version = null,
        effectiveDate = "November 21, 2026",
        introduction =
            "Wiccan treats historical spiritual material as education and reflection. This guidance applies to every " +
                "practical lesson, spell, potion, and optional practice in the app.",
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
                    "Published lessons, spells, and potions are practical step-by-step practices drawn from historical " +
                        "grimoires and ancient magical texts, each cited where available. AI-assisted drafts are published after " +
                        "an automated and AI review under the owner's authorization, which checks that each item is a full " +
                        "practice correctly cited to those sources, and includes safety, rights, originality, and wording checks.",
                    "Report a safety, rights, or accuracy concern through Contact Support. Content may be corrected or " +
                        "withdrawn while it is reviewed.",
                ),
            ),
        ),
    )

    val potionSafety = LegalDocument(
        id = LegalDocumentId.POTION_SAFETY,
        title = "Potion Safety",
        version = null,
        effectiveDate = "November 16, 2026",
        introduction =
            "Potion, infusion, bath, oil, spray, jar, tea, and skin-contact practices can involve real physical risks. " +
                "This guidance supplements each lesson's specific safety note. If a lesson is unclear, an ingredient is " +
                "unknown, or you cannot use it safely, do not make or use the preparation.",
        sections = listOf(
            LegalSection(
                "Ritual use and ingestion",
                listOf(
                    "Unless a lesson clearly identifies a preparation as ordinary food or drink made only with food-grade " +
                        "ingredients and normal kitchen methods, treat it as ritual use only and do not consume it.",
                    "Never ingest essential oils. Do not add essential oils, fragrance oils, craft herbs, crystals, unknown " +
                        "plants, non-food colorants, or ritual materials to food or drink.",
                    "A spiritual label such as potion, elixir, tea, or infusion does not establish that something is safe to " +
                        "swallow, inhale, apply to skin, or give to another person or animal.",
                ),
            ),
            LegalSection(
                "Ingredients, allergies, and health",
                listOf(
                    "Identify every ingredient before use and follow its manufacturer label. Do not use unknown, spoiled, " +
                        "foraged, contaminated, recalled, or hazardous substances.",
                    "Consider allergies, asthma, skin sensitivity, pregnancy or nursing, medications, medical conditions, and " +
                        "possible ingredient interactions. Ask an appropriate licensed health professional or pharmacist when " +
                        "you are unsure. Wiccan is not medical advice and no preparation is a diagnosis or treatment.",
                    "For skin contact, use only skin-safe ingredients at appropriate dilution and patch test a small area first. " +
                        "Stop immediately if irritation, pain, dizziness, breathing difficulty, or another adverse reaction occurs.",
                ),
            ),
            LegalSection(
                "Mixing, heating, bathing, and ventilation",
                listOf(
                    "Do not combine household cleaners, bleach, acids, bases, solvents, alcohol, medicines, or reactive " +
                        "materials. Never heat a sealed container or use a container that is not intended for the temperature " +
                        "and ingredients involved.",
                    "Keep flames and hot liquids on stable heat-safe surfaces, provide ventilation, tie back loose clothing and " +
                        "hair, and never leave heat or flame unattended.",
                    "Keep preparations out of eyes, mouth, mucous membranes, broken skin, and other sensitive areas. For baths, " +
                        "use a safe water temperature, prevent slips, and leave the bath if you feel faint, irritated, or unwell.",
                ),
            ),
            LegalSection(
                "Children, animals, and consent",
                listOf(
                    "Keep ingredients and finished preparations secured away from children and animals. Many plants, oils, " +
                        "foods, minerals, fumes, and topical products that adults tolerate can harm animals or children.",
                    "Do not give, apply, spray, burn near, or otherwise expose another person or animal to a preparation without " +
                        "informed consent and confirmation that the intended use is safe.",
                ),
            ),
            LegalSection(
                "Labeling, storage, spoilage, and disposal",
                listOf(
                    "Label every container with its ingredients, preparation date, intended use, and “Ritual use only — do not " +
                        "consume” when it is not food. Store it separately from food, drinks, medicines, and personal-care products.",
                    "Use clean equipment and suitable containers. Discard a preparation if its identity, age, storage history, " +
                        "or safety is uncertain, or if its color, odor, texture, pressure, or container changes unexpectedly.",
                    "Follow ingredient labels and local requirements for disposal. Do not pour oils, salts, concentrated herbs, " +
                        "chemicals, glitter, wax, or unknown mixtures into drains, soil, or natural water.",
                ),
            ),
            LegalSection(
                "Poisoning, injury, and emergencies",
                listOf(
                    "If someone may have swallowed, inhaled, or been exposed to something harmful, stop the practice and contact " +
                        "local emergency services or poison control. In the United States, Poison Control is available at " +
                        "1-800-222-1222 and poison.org. Do not wait for symptoms or rely on a ritual response.",
                    "Bring or photograph the product label or ingredient list for responders when it is safe to do so.",
                ),
            ),
            LegalSection(
                "Lesson-specific warnings and results",
                listOf(
                    "Always follow the lesson's activity-specific safety note and shared potion disclosure. This page does not " +
                        "override a stricter warning. If instructions conflict, choose the safer option or do not attempt the practice.",
                    "Wiccan does not promise or guarantee that any potion or related practice will produce a magical, spiritual, " +
                        "medical, psychological, fertility, relationship, financial, protective, or other result.",
                ),
            ),
        ),
    )
}
