package com.example.data

object DefaultData {
    val CHANNELS = listOf(
        DiscussionChannel(
            id = "anatomy_physiology",
            title = "Anatomy & Physiology",
            category = "General Science",
            description = "Discussions on body systems, muscular structures, and cardiac pathways essential for nurse licensure.",
            iconName = "favorite",
            onlineCount = 14
        ),
        DiscussionChannel(
            id = "midwifery_maternal",
            title = "Midwifery & Maternal Health",
            category = "Specialty Care",
            description = "Revision on antenatal care, stages of labor, obstetric emergencies, neonatal care, and UNMC midwifery standards.",
            iconName = "face",
            onlineCount = 28
        ),
        DiscussionChannel(
            id = "pediatrics_child",
            title = "Pediatrics & UNEPI",
            category = "Specialty Care",
            description = "Focus on pediatric nursing, childhood nutrition, illnesses, and Uganda's UNEPI immunization schedules.",
            iconName = "star",
            onlineCount = 19
        ),
        DiscussionChannel(
            id = "pharmacology_calc",
            title = "Pharmacology & Calculations",
            category = "Clinical Skills",
            description = "Mastering drug equations, IV drip rates, pediatric dosing formulas, side effects, and safe administration guidelines.",
            iconName = "settings",
            onlineCount = 37
        ),
        DiscussionChannel(
            id = "medical_surgical",
            title = "Medical-Surgical Review",
            category = "Clinical Skills",
            description = "Detailed pathways for nursing care plans, infectious diseases (Malaria, HIV/TB), and cardiovascular disorders.",
            iconName = "home",
            onlineCount = 25
        )
    )

    val PRELOAD_QUIZ_QUESTIONS = listOf(
        QuizQuestion(
            id = 1,
            subject = "Pharmacology",
            questionText = "A physician orders 500 mL of Normal Saline to be infused over 4 hours. The drop factor of the IV tubing is 15 drops/mL. What should the drip rate be in drops per minute?",
            optionA = "15 drops/min",
            optionB = "31 drops/min",
            optionC = "45 drops/min",
            optionD = "62 drops/min",
            correctOption = "B",
            explanation = "Using the standard IV drip formula: Drip Rate = (Total Volume in mL * Drop Factor) / Time in Minutes.\n\nVolume = 500 mL\nDrop Factor = 15\nTime = 4 hours * 60 minutes = 240 minutes.\n\nCalculations:\nDrip Rate = (500 * 15) / 240 = 7500 / 240 = 31.25 drops/min.\nRounding to the nearest whole drop yields approximately 31 drops/min."
        ),
        QuizQuestion(
            id = 2,
            subject = "Midwifery",
            questionText = "A pregnant mother at 36 weeks gestation presents at the Mulago antenatal clinic with severe headache, blurred vision, and a blood pressure of 160/110 mmHg. What is the most likely diagnosis?",
            optionA = "Gestational hypertension",
            optionB = "Chronic hypertension",
            optionC = "Severe Pre-eclampsia",
            optionD = "Eclampsia",
            correctOption = "C",
            explanation = "Severe Pre-eclampsia is diagnosed when severe gestational hypertension (systolic BP >= 160 mmHg or diastolic BP >= 110 mmHg) occurs on two occasions after 20 weeks of gestation, accompanied by signs of end-organ damage such as visual disturbances (blurred vision), severe persistent headaches, or significant proteinuria. If seizures were present, it would be classified as Eclampsia."
        ),
        QuizQuestion(
            id = 3,
            subject = "Pediatrics",
            questionText = "Under the Uganda National Expanded Programme on Immunisation (UNEPI) schedule, at what age is the Measles-Rubella (MR) vaccine first administered?",
            optionA = "At Birth",
            optionB = "At 6 Weeks",
            optionC = "At 9 Months",
            optionD = "At 18 Months",
            correctOption = "C",
            explanation = "Under the UNEPI guidelines in Uganda, the first dose of the Measles-Rubella (MR) vaccine is administered at 9 months of age. The second dose is given as a booster at 18 months of age."
        ),
        QuizQuestion(
            id = 4,
            subject = "Anatomy",
            questionText = "Which heart valve prevents the backflow of oxygenated blood from the left ventricle into the left atrium during ventricular systole?",
            optionA = "Mitral (Bicuspid) Valve",
            optionB = "Tricuspid Valve",
            optionC = "Aortic Semilunar Valve",
            optionD = "Pulmonary Semilunar Valve",
            correctOption = "A",
            explanation = "The Mitral (or Bicuspid) valve is located between the left atrium and left ventricle. During ventricular systole, the left ventricle contracts to pump blood out to the aorta, and the Mitral valve closes to construct a safe barrier, preventing backflow of blood back into the left atrium."
        ),
        QuizQuestion(
            id = 5,
            subject = "Pharmacology",
            questionText = "Which of the following is the standard first-line treatment recommended by the Ugandan Ministry of Health for uncomplicated Plasmodium falciparum malaria?",
            optionA = "Artemether-Lumefantrine (Coartem)",
            optionB = "Oral Quinine",
            optionC = "IV Artesunate",
            optionD = "Chloroquine",
            correctOption = "A",
            explanation = "Artemether-Lumefantrine (commercially Coartem) is the standard first-line Artemisinin-based Combination Therapy (ACT) recommended by the Ugandan Ministry of Health for uncomplicated malaria. For severe/complicated malaria, Intravenous Artesunate is the first-line treatment."
        )
    )

    fun getPreloadMessages(channelId: String): List<DiscussionMessage> {
        return when (channelId) {
            "pharmacology_calc" -> listOf(
                DiscussionMessage(
                    channelId = "pharmacology_calc",
                    senderName = "Okello Moses",
                    senderField = "General Nursing Student",
                    avatarColor = 0xFF00796B.toInt(),
                    text = "Hello colleagues! Can someone explain the shorthand formula for microgtt/min calculations? I always get confused with the factor of 60."
                ),
                DiscussionMessage(
                    channelId = "pharmacology_calc",
                    senderName = "Nakitende Grace",
                    senderField = "Nurse Educator",
                    avatarColor = 0xFFC2185B.toInt(),
                    text = "Aha, Okello! Remember that for microdrip (microgtt) tubing, the drop factor is always 60 drops/mL. Because 1 hour is 60 minutes, the drop factor and minutes cancel each other out! So, mL/hour is mathematically EXACTLY equal to microgtt/min."
                ),
                DiscussionMessage(
                    channelId = "pharmacology_calc",
                    senderName = "Aisha Namuganza",
                    senderField = "Midwifery Student",
                    avatarColor = 0xFF7B1FA2.toInt(),
                    text = "Wow Nakitende! That makes it so simple! So if a doctor orders 80 mL/hr, it is simply 80 microgtt/min? That's a great shortcut for UNMC exams!"
                )
            )
            "midwifery_maternal" -> listOf(
                DiscussionMessage(
                    channelId = "midwifery_maternal",
                    senderName = "Nsubuga Derrick",
                    senderField = "Midwifery Student",
                    avatarColor = 0xFF1976D2.toInt(),
                    text = "Hi all! I am reviewing the active management of third stage of labor (AMTSL). What are the three core interventions we must write down?"
                ),
                DiscussionMessage(
                    channelId = "midwifery_maternal",
                    senderName = "Nakitende Grace",
                    senderField = "Nurse Educator",
                    avatarColor = 0xFFC2185B.toInt(),
                    text = "Hello Derrick! Perfect question. The 3 pillars are:\n1. Administration of a uterotonic drug (ideally oxytocin 10 IU IM within 1 minute of fetal birth).\n2. Controlled cord traction (CCT) with counter-traction on the uterus.\n3. Uterine massage immediately after placenta delivery, repeated every 15 mins for the first 2 hours."
                )
            )
            else -> listOf(
                DiscussionMessage(
                    channelId = channelId,
                    senderName = "StudyGram BOT",
                    senderField = "System Assistant",
                    avatarColor = 0xFF388E3C.toInt(),
                    text = "Welcome to the $channelId discussion room! Start typing your questions below, or tap the quiz toggle to attempt practice revision cards."
                )
            )
        }
    }

    val MOCK_PEERS = listOf(
        UserProfile(id = "p1", username = "Namuli Catherine", nursingField = "Midwifery", avatarColor = 0xFFC2185B.toInt()),
        UserProfile(id = "p2", username = "Mugisha Paul", nursingField = "General Nursing", avatarColor = 0xFF1976D2.toInt()),
        UserProfile(id = "p3", username = "Aisha Namuganza", nursingField = "Pediatrics", avatarColor = 0xFF7B1FA2.toInt()),
        UserProfile(id = "p4", username = "Kiiza Simon", nursingField = "Education", avatarColor = 0xFF388E3C.toInt()),
        UserProfile(id = "p5", username = "Nakitende Grace", nursingField = "Surgical", avatarColor = 0xFF00796B.toInt())
    )
}
