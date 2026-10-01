package com.example.data.audio

import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import java.util.LinkedList

enum class SidhuVoiceStyle(val label: String, val pitchMultiplier: Float, val speedMultiplier: Float) {
    ENERGETIC_JOSH("🔥 Sidhu Josh (Male Baritone)", 0.82f, 0.88f),
    SHAYARI_PUNCH("📜 Sidhu Shayari (Deep Male)", 0.78f, 0.84f),
    TV_BROADCAST("🎙️ Star TV Commentator", 0.85f, 0.90f)
}

object SidhuCommentaryGenerator {

    // Anti-repetition memory: Keeps track of recent phrases so viewers NEVER hear repeats
    private val recentPhrases = LinkedList<String>()
    private const val MAX_HISTORY = 18

    private val screamingOpeners = listOf(
        "ओए गुरुउउउ!",
        "अरे बाप रे बाप!",
        "गया गया गया! डंडा उखड़ गया गुरु!",
        "चक दे फट्टे, नप दे किल्ली!",
        "खटाक! धमाका हो गया गुरु!",
        "ओए होए होए! क्या देखा!",
        "हैट्रिक! हैट्रिक! हैट्रिक!",
        "शेर की दहाड़! ठोको ताली गुरु!",
        "मायानगरी में भूचाल आ गया गुरु!",
        "ओए गुरु! सांसें रोक लो!"
    )

    private val openers = listOf(
        "Oye guru khataak!",
        "Thoko taali guru!",
        "Arey bahiya suniye!",
        "Chak de phatte!",
        "Oye hoye hoye!",
        "Arey baap re baap!",
        "Dhamaka ho gaya guru!",
        "Taram-tar ho gaya maidan!",
        "Kya baat hai, kya baat hai!",
        "Kamaal kar ditta guru!",
        "Sher ki dahaad dekho!",
        "Maidan mein aag lag gayi!",
        "Zabardast nazara guru!",
        "Wah bhai wah, maza aa gaya!",
        "Arey bahiya dekhte jao!",
        "Taaliyan bajti rehni chahiye!",
        "Kanoon ki tarah teekha shot!",
        "Guru dil garden garden ho gaya!"
    )

    private val closers = listOf(
        "Thoko taali!",
        "Taaliyan rukni nahi chahiye!",
        "Bowler ko paani pilao re koi!",
        "Maza aa gaya kasam se!",
        "Chak de phatte, nap de killi!",
        "Ballebaaz ne system hila diya!",
        "Bowler ki to hawa nikal gayi!",
        "Darshak jhoom uthe hain!",
        "Aise shot roz dekhne ko nahi milte!",
        "Sunehra itihas likha ja raha hai!",
        "Guru dil khush kar ditta!",
        "Ye match lambe samay tak yaad rahega!"
    )

    private val sixesMetaphors = listOf(
        "Gend gayi stadium ke paar taram-tar! Seedhe car parking mein!",
        "Aasman mein chhed kar diya! Gend seedhe chandrama par ja kar giregi!",
        "Ye ball nahi thi guru, ye toh agni missile thi! Sher ka waar!",
        "Chidya udd gayi aasmaan mein! Ballebaaz ne dande se dhoom macha di!",
        "Gend taaron se baat kar rahi hai! Airplane mode par chali gayi!",
        "Darshak bane fielder aur fielder bane darshak! 90 meter ka chhakka!",
        "Helicopter shot! Gend hawa mein tairti hui seema rekha ke das gaj paar!",
        "Aisa chhakka maara ki umpire bhi haath utha kar bhangra karne laga!",
        "Gend gayi satellite ke paas! NASA wale dhoondenge ab isko!",
        "Ballebaaz ne bowler ke hosle pash-pash kar diye! Gagan-chumbi sixer!",
        "Gend ko stadium ke chat par pahuncha diya! Nayi gend mangwao umpire sahab!",
        "Goli ki raftaar se ball aayi aur rocket ban kar chali gayi! Kamaal ka chhakka!",
        "Taakat aur timing ka lajawab sangam! Gend border paar, chhakka!"
    )

    private val foursMetaphors = listOf(
        "Gend seema rekha ke paar, char run! Jaise bullet train nikal gayi!",
        "Gend ko dho daala, jaise dhobi ghat pe sabun laga ke kapde!",
        "Makhan jaisa shot! Na koi taakat, sirf nazakat aur class! Chauka!",
        "Zameen ko choomti hui gend carpet ki tarah seedhi boundary par!",
        "Ballebaaz ne aisi goli maari ki cover fielder hil bhi na paya! Char run!",
        "Bowler ki line aur length ka bharta bana diya! Zabardast chauka!",
        "Gap dhoonda aur gend ko rocket bana kar char run bator liye!",
        "Jaise garam churi makhan mein se nikalti hai, waise hi chauka nikala!",
        "Fielder dekhta reh gaya munh khol kar, gend boundary paar ho gayi!",
        "Class, timing aur royalty! Ballebaaz ka classic cover drive chauka!",
        "Gend gayi boundary rope se takra kar! Umpire ne char ungliyan dikhayi!",
        "Bowler ki aankhon mein dhool jhonk di! Shaandar chauka!"
    )

    private val wicketsMetaphors = listOf(
        "Dandi ud gayi! Gilliyan hawa mein bhangra pa rahi hain! Clean bowled!",
        "Ballebaaz aise gaya jaise railway station se express train choot gayi ho!",
        "Jaise garam tawa pe paani chhidko, waise hi wicket gir gaya! Out!",
        "Khel khatam, paisa hazam! Bowler ne aisi lehar maari ki batti gul ho gayi!",
        "Ballebaaz ko pavilion ka rasta dikha diya! Tata, bye-bye, khatam!",
        "Kanoon ke haath lambe hote hain aur bowler ki in-swinging delivery teekhi! Out!",
        "Ballebaaz ke hosle past aur bowler ke taaron mein josh! Wicket gir gaya!",
        "Cycle ke tyre se hawa nikal gayi jaise, waise hi ballebaaz ki inning khatam!",
        "Umpire ki ungli aasmaan ki taraf! Ballebaaz be-bas, out ho kar laut te hue!",
        "Fielder ne boundary line par cheete ki tarah lapka! Shaandar catch out!",
        "Stumps khadak gaye guru! Bowler ne middle stump ukhad diya!"
    )

    private val singlesMetaphors = listOf(
        "Halke haathon se push kiya, ek run chura liya jaise billi doodh pee jaye!",
        "Bohot khoob, chidiya chug gayi khet! Ballebaazon ne ek run pura kiya!",
        "Samajhdari bhari ballebaazi! Single le kar strike rotate kar li!",
        "Gend gayi gap mein, daud kar ek run aasani se bator liya!",
        "Boond boond se sagar banta hai! Ek run score mein joda!",
        "Tez daude aur crease par dive lagayi, ek run surakshit pura kiya!",
        "Scoreboard chalta rehna chahiye, ek ahem single run!"
    )

    private val doublesMetaphors = listOf(
        "Oye cheete ki chaal aur baaz ki nazar! Daud kar do run poore kar liye!",
        "Ballebaaz daud rahe hain jaise peeche police lagi ho! Do run mil gaye!",
        "Kamaal ki running between the wickets! Bowler dekhta hi reh gaya, do run!",
        "Fielding thodi sust aur ballebaaz chust! Do run asani se nikal liye!",
        "Fielder ne ball pick ki tab tak do run poore ho chuke the! Behtareen taalmel!"
    )

    private val dotsMetaphors = listOf(
        "Khaata na bahi, jo bowler kahe wahi sahi! Dot ball!",
        "Gend aur balle ka koi mel nahi! Jaise tel aur paani! Dot ball!",
        "Ballebaaz ne gend ko samman diya jaise damaad ko sasural mein milta hai! Dot ball!",
        "Bowler ki tedi kheer! Ballebaaz bilkul be-bas! Koi run nahi!",
        "Seedhi gend wicketkeeper ke dastano mein! Ballebaaz chakma kha gaya! Dot ball!",
        "Suraksha pehle aur run baad mein! Ballebaaz ne block kiya! Dot ball!",
        "Bowler ne jaal bichhaya tha par ballebaaz ne gend ko jaane diya! Dot delivery!"
    )

    private val extrasMetaphors = listOf(
        "Arey bahiya ye to highway nikal gaya! Dismil dismil door! Wide ball!",
        "Bowler bhatak gaya, free fund ka ek run gift kar diya! Wide ball!",
        "Laxman rekha paar kar li bowler ne! No ball aur free hit mil gayi! Thoko taali!",
        "Gend leg stump se bhatakti hui nikal gayi! Umpire ne bahein failayi, wide ball!",
        "Bowler ka foot line se aage! No ball! Ballebaaz ke paas ab license hai udne ka!"
    )

    private val shayaris = listOf(
        "Taaron mein chamak, dilon mein shola! Ballebaaz ne aisi gend ko goli ki tarah dho daala!",
        "Koshish karne walon ki kabhi haar nahi hoti! Aur Sidhu paaji ki commentary ke aage koi baat nahi hoti!",
        "Loha garm tha aur hathoda mar diya! Ballebaaz ne boundary par shor macha diya!",
        "Gendbaaz ne phenka jaal, ballebaaz ne kiya kamaal! Thoko taali guru!",
        "Patte hile bina hawa nahi chalti, aur Sidhu paaji ke bina cricket ki mehfil nahi jamti!",
        "Ghee nikalna hai toh ungli tedi karni padti hai, ballebaaz ne shot aisa mara ki bowler ki batti gul!",
        "Sher boodha ho sakta hai guru, par ghaas kabhi nahi khata! Kya lajawab shot hai!",
        "Aandhi chale ya toofan, ballebaaz khada hai ban kar chattan!",
        "Suraj ko roshni aur chand ko chaandni, ballebaaz ne bowler ko sikha di naani!",
        "Jo darr gaya samjho mar gaya, par ye ballebaaz toh boundary paar kar gaya!",
        "Chidiya pankh failaye toh aasmaan chhota lagta hai, aur ye ballebaaz bat ghumaye toh ground chhota padta hai!",
        "Khatron se khelna sher ka kaam hai, aur aisi ball par chhakka maarna is ballebaaz ka kaam hai!",
        "Dariya ki leharon se darr kar nauka paar nahi hoti, himmat karne walon ki haar nahi hoti!",
        "Baag mein phool khilte hain bahar aane se, aur maidan mein shor machata hai ballebaaz ke chhakke lagane se!",
        "Waqt aane pe bata denge tujhe aye aasmaan, hum abhi se kya batayein kya hamare dil mein hai! Thoko taali!",
        "Jab hausla bana liya unchi udaan ka, fir dekhna fizool hai kad aasmaan ka! Kamaal ka shot guru!",
        "Aandhi roke toh hum toofan, dariya roke toh hum chattan! Kya josh aur kya tevar!",
        "Haath kangan ko aarsi kya, aur padhe likhe ko faarsi kya! Gend seema paar, thoko taali!",
        "Zindagi mein risk nahi toh ishq nahi guru! Ballebaaz ne risk liya aur boundary paar bhej diya!",
        "Khoda pahad nikli chuhiya, bowler ka munh ban gaya pudia! Clean bowled guru!",
        "Jiska kaam usi ko saaje, ballebaaz ke bat se chaaron taraf baaje hi baaje!",
        "Paani mein aag laga di guru! Gend ko bheja seedhe darshak deergah mein!",
        "Sher ki maand mein ghus kar shikaar kiya hai guru! Aisa shot mara ki sab dekhte reh gaye!",
        "Guru khataak! Na no-ball na wide, seedhi gend aur bails ud gayi outside!",
        "Jo bhara nahi hai bhavon se behti jisme rasdhar nahi, wo hriday nahi wo pathar hai jise is chhakke se pyar nahi!",
        "Hawaon ke bharose mat udd, chattanein toofanon ka rukh mod deti hain! Aur ye shot match ka rukh mod deta hai!",
        "Manzil unhi ko milti hai jinke sapno mein jaan hoti hai, pankhon se kuch nahi hota hauslon se udaan hoti hai!",
        "Suraj dhalne se pehle jo baazi palat de wahi sikandar! Thoko taali guru!",
        "Chamak chaand se hoti hai sitaron se nahi, aur jeet unhi ki hoti hai jo thakte haarne se nahi!",
        "Na talwar ki dhaar se na goliyon ki bauchaar se, bowler ghayal ho gaya is karare shot ke prahaar se!",
        "Dillagi dilon ka khel hai, aur cricket chaunkon chhakon ka mela hai guru! Thoko taali!"
    )

    // Helper to pick a phrase that wasn't used recently
    private fun pickFresh(pool: List<String>): String {
        val candidates = pool.filter { it !in recentPhrases }
        val chosen = if (candidates.isNotEmpty()) candidates.random() else pool.random()
        recentPhrases.add(chosen)
        if (recentPhrases.size > MAX_HISTORY) {
            recentPhrases.removeFirst()
        }
        return chosen
    }

    fun generateBallCommentary(
        ball: BallEventEntity,
        strikerName: String,
        bowlerName: String,
        currentScore: String,
        style: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH
    ): String {
        val batsman = strikerName.ifBlank { "Ballebaaz" }
        val bowler = bowlerName.ifBlank { "Bowler" }

        val opener = pickFresh(openers)
        val closer = pickFresh(closers)

        val styleBonus = when (style) {
            SidhuVoiceStyle.SHAYARI_PUNCH -> "${pickFresh(shayaris)} "
            SidhuVoiceStyle.ENERGETIC_JOSH -> if (ball.runs >= 4 || ball.isWicket || (ball.runs == 0 && Math.random() < 0.2)) "${pickFresh(shayaris)} " else ""
            SidhuVoiceStyle.TV_BROADCAST -> "AyuuCric Live exclusive, "
        }

        val isGenuineNoBall = ball.extraType.equals("NoBall", ignoreCase = true) || ball.extraType.equals("NB", ignoreCase = true)
        val isWide = ball.extraType.equals("Wide", ignoreCase = true) || ball.extraType.equals("WD", ignoreCase = true)
        val isLegBye = ball.extraType.equals("LegBye", ignoreCase = true) || ball.extraType.equals("LB", ignoreCase = true)
        val isBye = ball.extraType.equals("Bye", ignoreCase = true) || ball.extraType.equals("B", ignoreCase = true)

        val scoreVoice = if (currentScore.contains("/")) {
            val parts = currentScore.split("/")
            val r = parts.getOrNull(0)?.trim() ?: "0"
            val w = parts.getOrNull(1)?.trim() ?: "0"
            "$r run, $w wicket"
        } else if (!currentScore.contains("run", ignoreCase = true)) {
            "$currentScore run"
        } else {
            currentScore
        }

        return when {
            // Wicket dismissal
            ball.isWicket -> {
                val metaphor = pickFresh(wicketsMetaphors)
                "$opener $styleBonus $bowler ne $batsman ko pavilion bhej diya! $metaphor Score hai $scoreVoice! $closer"
            }
            // No-Ball deliveries (Overstepping / Above waist)
            isGenuineNoBall -> {
                when {
                    ball.runs >= 6 || ball.isSix -> {
                        val dialogues = listOf(
                            "$opener Oye chak de phatte guru! No-ball par gagan-chumbi aasmaani chhakka! Kul 7 run mil gaye aur ab agli gend par Free Hit bhi milegi! Ballebaaz ki lottery lag gayi! Score $scoreVoice! $closer",
                            "$opener Arey baap re baap! Chhakka aur No-ball dono ek sath guru! Free hit ka inaam aur 7 run khate mein! Score $scoreVoice! $closer"
                        )
                        pickFresh(dialogues)
                    }
                    ball.runs == 4 -> {
                        "$opener No-ball aur sath mein karara chauka! 5 run mil gaye aur agle ball par Free Hit ka bumper offer! Score $scoreVoice! $closer"
                    }
                    ball.runs > 0 -> {
                        "$opener No-ball! Ballebaaz ne daud kar ${ball.runs} run bhi liye! Kul ${ball.runs + 1} run khate mein aur agle ball par Free Hit! Score $scoreVoice."
                    }
                    else -> {
                        "$opener Bowler ne crease overstep kar di, No-ball ka ishaara! Batting team ko 1 run aur agli ball par Free Hit! Score $scoreVoice."
                    }
                }
            }
            // Wide deliveries
            isWide -> {
                if (ball.runs > 0) {
                    "$opener Wide ball aur sath mein ${ball.runs} extra run! Kul ${ball.runs + 1} run batting team ke khate mein! Score $scoreVoice."
                } else {
                    val metaphor = pickFresh(extrasMetaphors)
                    "$opener $bowler ki taraf se Wide ball! $metaphor Free fund ka 1 run mila, Score $scoreVoice!"
                }
            }
            // Byes / Leg Byes
            isLegBye || isBye -> {
                val label = if (isLegBye) "Leg-bye" else "Bye"
                "$opener Bat par nahi lagi, $label ke roop mein ${if (ball.runs > 0) "${ball.runs} run" else "1 run"} bator liye! Score $scoreVoice."
            }
            // Legal deliveries off the bat
            ball.runs == 6 -> {
                val metaphor = pickFresh(sixesMetaphors)
                "$opener $styleBonus $batsman ke bat se nikla chhakka! $metaphor Score pahuncha $scoreVoice! $closer"
            }
            ball.runs == 4 -> {
                val metaphor = pickFresh(foursMetaphors)
                "$opener $styleBonus $batsman ne jad diya lajawab chauka! $metaphor Score $scoreVoice! $closer"
            }
            ball.runs == 3 -> {
                "$opener Shandar running between the wickets! $batsman ne 3 run tezi se daud kar pure kiye! Score $scoreVoice."
            }
            ball.runs == 2 -> {
                val metaphor = pickFresh(doublesMetaphors)
                "$opener $batsman ne $bowler ki gend par do run churaye! $metaphor Score $scoreVoice."
            }
            ball.runs == 1 -> {
                val metaphor = pickFresh(singlesMetaphors)
                "$batsman ne ek run liya. $metaphor Score $scoreVoice."
            }
            ball.extraType?.isNotEmpty() == true && ball.extraType != "None" -> {
                val metaphor = pickFresh(extrasMetaphors)
                "$opener $bowler ki taraf se $metaphor Score $scoreVoice!"
            }
            else -> {
                val metaphor = pickFresh(dotsMetaphors)
                "$bowler ki kassi hui delivery! $metaphor Score $scoreVoice par kayam."
            }
        }
    }

    fun generateHatTrickCommentary(
        bowlerName: String,
        strikerName: String,
        wicketType: String,
        currentScore: String,
        style: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH
    ): String {
        val shout = pickFresh(screamingOpeners)
        val closer = pickFresh(closers)
        val hatTrickLines = listOf(
            "$shout हैट्रिक! हैट्रिक! हैट्रिक! गेंदबाज $bowlerName ने लगातार तीन गेंदों पर तीन विकेट लेकर इतिहास रच दिया! $strikerName $wicketType होकर पवेलियन लौटते हुए! डंडियों का कबाड़खाना बना दिया गुरु! $closer",
            "$shout अरे बाप रे बाप! क्या देख लिया मैंने! $bowlerName की हैट्रिक! लगातार तीन विकेट! चक्रव्यूह तोड़ दिया गुरु! स्टेडियम में भूचाल आ गया! Score $currentScore! चक दे फट्टे, नप दे किल्ली!",
            "$shout इतिहास के सुनहरे पन्नों में दर्ज हो गया ये पल! गेंदबाज $bowlerName ने ली हैट्रिक! $strikerName को हवा भी नहीं लगी! ठोको ताली गुरु, ठोको ताली!"
        )
        return pickFresh(hatTrickLines)
    }

    fun generateHatTrickBallChanceCommentary(
        bowlerName: String,
        strikerName: String,
        currentScore: String,
        style: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH
    ): String {
        val shout = pickFresh(screamingOpeners)
        val lines = listOf(
            "$shout सांसें थाम लीजिए गुरु! लगातार दो गेंदों पर दो विकेट! अब आ गई हैट्रिक गेंद! $bowlerName के सामने $strikerName! स्टेडियम में सन्नाटा और दिल की धड़कनें तेज!",
            "$shout ओए गुरु! क्या अगली गेंद पर हैट्रिक होगी? गेंदबाज $bowlerName शेर की तरह दहाड़ रहा है! पूरा मैदान खड़ा होकर ताली बजा रहा है! ठोको ताली!"
        )
        return pickFresh(lines)
    }

    fun generateTurningPointCommentary(
        turningType: String,
        bowlerName: String,
        strikerName: String,
        currentScore: String,
        detail: String = "",
        style: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH
    ): String {
        val shout = pickFresh(screamingOpeners)
        val closer = pickFresh(closers)
        return when (turningType) {
            "OVER_WICKET_STORM" -> {
                "$shout मैच का सबसे बड़ा टर्निंग पॉइंट गुरु! एक ही ओवर में दूसरा झटका! $bowlerName ने मैच का रुख पलट कर रख दिया! $strikerName पवेलियन जाते हुए! $closer"
            }
            "TIGHT_CHASE" -> {
                "$shout सांसें रोक देने वाला रोमांच! $detail! हर गेंद पर दिल की धड़कन बढ़ रही है गुरु! कौन बनेगा सिकंदर और कौन हारेगा बाज़ी! $closer"
            }
            "LAST_OVER" -> {
                "$shout ओए गुरु! अंतिम ओवर का महा-मुकाबला! $detail! एक तरफ जीत, दूसरी तरफ हार, बीच में खड़ा है ये दिलेर खिलाड़ी! $closer"
            }
            "LAST_BALL" -> {
                "$shout अंतिम गेंद का सांस रोक देने वाला थ्रिलर! $detail! जो डरा वो मरा, जो लड़ा वो जीता! $closer"
            }
            "BATSMAN_FIFTY" -> {
                "$shout शेर की दहाड़! $strikerName ने 50 रन की तूफानी फिफ्टी जड़ दी गुरु! बल्ला उठाकर दर्शकों का अभिवादन स्वीकार करते हुए! कमाल कर दित्ता! $closer"
            }
            "BATSMAN_CENTURY" -> {
                "$shout महा-शतक! 100 रन पूरे! $strikerName का ऐतिहासिक शतक! मायानगरी में आतिशबाजी हो रही है गुरु! नतमस्तक हो गया पूरा स्टेडियम! $closer"
            }
            else -> {
                "$shout मैच ने पलटी मारी है गुरु! $detail! पासा पलट गया! जो टीम नीचे थी वो ऊपर आ गई! $closer"
            }
        }
    }

    fun generateDrsCommentary(verdict: String, player: String): String {
        val opener = pickFresh(openers)
        return if (verdict.contains("OUT", ignoreCase = true) && !verdict.contains("NOT OUT", ignoreCase = true)) {
            val metaphor = pickFresh(wicketsMetaphors)
            "$opener Guru faisla aa gaya! Third umpire ne laal jhandi dikha di! $player OUT hain! $metaphor Thoko taali!"
        } else {
            "$opener Oye bach gaye guru! Third umpire ne kaha NOT OUT! $player abhi maidan par sher ki tarah datey rahenge! Ballebaaz ki jaan bachi, lakhon paye! Thoko taali!"
        }
    }

    fun generateMatchSituation(match: MatchEntity): String {
        val overs = "${match.legalBalls / 6}.${match.legalBalls % 6}"
        val shayari = pickFresh(shayaris)
        return "Guru suniye AyuuCric Live ka scorecard! $shayari ${match.teamA} banaam ${match.teamB}! Score hai ${match.score}/${match.wickets} in $overs overs! ${match.statusDetail}! Match mein aag lagi hui hai, dono team sher ki tarah lad rahi hain!"
    }
}
