package com.example.data.audio

object HindiSpeechFormatter {

    /**
     * Converts mixed Hinglish / Latin cricket commentary into fluent Devanagari Hindi
     * so that Android Google Text-to-Speech pronounces every single word with 100% natural,
     * crisp Hindi pronunciation instead of reading Latin letters as broken English.
     */
    fun formatForHindiTts(input: String): String {
        if (input.isBlank()) return ""

        var text = input

        // Clean emojis & markdown characters that confuse TTS
        text = text.replace(Regex("[*#_`~]"), "")
        text = text.replace(Regex("[🎙️🔥📜⚡🌟🎯🏏👏🏆🤖📴🔑💾]"), "")

        // Sidhu Paaji trademark exclamations to pure Devanagari
        val replacements = listOf(
            Regex("(?i)\\bOye\\s+guru\\s+khataak\\b") to "ओए गुरु खटाक!",
            Regex("(?i)\\bOye\\s+guru\\b") to "ओए गुरु!",
            Regex("(?i)\\bThoko\\s+taali\\s+guru\\b") to "ठोको ताली गुरु!",
            Regex("(?i)\\bThoko\\s+taali\\b") to "ठोको ताली!",
            Regex("(?i)\\bChak\\s+de\\s+phatte[\\s,]*nap\\s+de\\s+killi\\b") to "चक दे फट्टे, नप दे किल्ली!",
            Regex("(?i)\\bChak\\s+de\\s+phatte\\b") to "चक दे फट्टे!",
            Regex("(?i)\\bKhataak\\b") to "खटाक!",
            Regex("(?i)\\bArey\\s+bahiya\\b") to "अरे भईया",
            Regex("(?i)\\bOye\\s+hoye\\s+hoye\\b") to "ओए होए होए!",
            Regex("(?i)\\bArey\\s+baap\\s+re\\s+baap\\b") to "अरे बाप रे बाप!",
            Regex("(?i)\\bDil\\s+garden\\s+garden\\b") to "दिल गार्डन गार्डन",
            Regex("(?i)\\bTaram[- ]?tar\\b") to "तरम-तर",
            Regex("(?i)\\bWah\\s+bhai\\s+wah\\b") to "वाह भाई वाह!",
            Regex("(?i)\\bMaza\\s+aa\\s+gaya\\b") to "मज़ा आ गया!",
            Regex("(?i)\\bKamaal\\s+kar\\s+ditta\\b") to "कमाल कर दित्ता गुरु!",
            Regex("(?i)\\bBallebaaz\\b") to "बल्लेबाज़",
            Regex("(?i)\\bBallebaazi\\b") to "बल्लेबाज़ी",
            Regex("(?i)\\bGendbaaz\\b") to "गेंदबाज़",
            Regex("(?i)\\bGendbaazi\\b") to "गेंदबाज़ी",
            Regex("(?i)\\bBowler\\b") to "गेंदबाज़",
            Regex("(?i)\\bBatsman\\b") to "बल्लेबाज़",
            Regex("(?i)\\bChhakka\\b") to "छक्का",
            Regex("(?i)\\bChhakke\\b") to "छक्के",
            Regex("(?i)\\bSixes\\b") to "छक्के",
            Regex("(?i)\\bSix\\b") to "छक्का",
            Regex("(?i)\\bChauka\\b") to "चौका",
            Regex("(?i)\\bChauke\\b") to "चौके",
            Regex("(?i)\\bFours\\b") to "चौके",
            Regex("(?i)\\bFour\\b") to "चौका",
            Regex("(?i)\\bWicket\\b") to "विकेट",
            Regex("(?i)\\bWickets\\b") to "विकेट",
            Regex("(?i)\\bOut\\b") to "आउट",
            Regex("(?i)\\bRuns\\b") to "रन",
            Regex("(?i)\\bRun\\b") to "रन",
            Regex("(?i)\\bOvers\\b") to "ओवर",
            Regex("(?i)\\bOver\\b") to "ओवर",
            Regex("(?i)\\bBoundary\\b") to "बाउंड्री",
            Regex("(?i)\\bStadium\\b") to "स्टेडियम",
            Regex("(?i)\\bMaidan\\b") to "मैदान",
            Regex("(?i)\\bScorecard\\b") to "स्कोरकार्ड",
            Regex("(?i)\\bScoreboard\\b") to "स्कोरबोर्ड",
            Regex("(?i)\\bScore\\b") to "स्कोर",
            Regex("(?i)\\bMatch\\b") to "मैच",
            Regex("(?i)\\bLive\\b") to "लाइव",
            Regex("(?i)\\bDot\\s+ball\\b") to "डॉट गेंद",
            Regex("(?i)\\bWide\\s+ball\\b") to "वाइड गेंद",
            Regex("(?i)\\bNo\\s+ball\\b") to "नो बॉल",
            Regex("(?i)\\bFree\\s+hit\\b") to "फ्री हिट",
            Regex("(?i)\\bTarget\\b") to "टारगेट",
            Regex("(?i)\\bAyuuCric\\b") to "आयु क्रिक"
        )

        for ((regex, replacement) in replacements) {
            text = text.replace(regex, replacement)
        }

        // Add pause indicators for natural rhythmic breathing between sentences
        text = text.replace(".", ". ")
            .replace("!", "! ")
            .replace("?", "? ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return text
    }
}
