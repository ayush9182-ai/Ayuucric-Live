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
        "Baag mein phool khilte hain bahar aane se, aur maidan mein shor machata hai ballebaaz ke chhakke lagane se!"
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
            SidhuVoiceStyle.ENERGETIC_JOSH -> ""
            SidhuVoiceStyle.TV_BROADCAST -> "AyuuCric Live exclusive, "
        }

        return when {
            // Six on a No-Ball combo (Chhakka + No-Ball / 7 runs)
            (ball.extraType.equals("NoBall", ignoreCase = true) || ball.extraType?.contains("No", ignoreCase = true) == true) && (ball.isSix || ball.runs >= 6) ||
            (ball.runs == 7 && (ball.extraType?.isNotEmpty() == true)) -> {
                val sixNoBallDialogues = listOf(
                    "$opener Oye chak de phatte guru! No-ball par gagan-chumbi aasmaani chhakka! Gend stadium ke paar car parking mein! Kul 7 run mil gaye aur ab agli gend par Free Hit bhi milegi! Ballebaaz ki lottery lag gayi, bowler ke hosh udd gaye! Thoko taali guru! $closer",
                    "$opener Arey baap re baap! Chhakka aur No-ball dono ek sath! Ye to double dhamaka ho gaya guru! Free hit ka inaam aur 7 run khate mein! Bowler ki to rooh kaanp gayi hogi! $closer",
                    "$opener Oye hoye hoye! No ball par aag ugalta hua chhakka! Ballebaaz ne bowler ki line-length ka dahi bada bana diya! Agli ball par free hit, ball stadium se bahar! Thoko taali!"
                )
                pickFresh(sixNoBallDialogues)
            }
            ball.isWicket -> {
                val metaphor = pickFresh(wicketsMetaphors)
                "$opener $styleBonus $bowler ne $batsman ko pavilion bhej diya! $metaphor Score hai $currentScore! $closer"
            }
            ball.runs == 6 -> {
                val metaphor = pickFresh(sixesMetaphors)
                "$opener $styleBonus $batsman ke bat se nikla chhakka! $metaphor Score pahuncha $currentScore! $closer"
            }
            ball.runs == 4 -> {
                val metaphor = pickFresh(foursMetaphors)
                "$opener $styleBonus $batsman ne jad diya lajawab chauka! $metaphor Score $currentScore! $closer"
            }
            ball.runs == 2 -> {
                val metaphor = pickFresh(doublesMetaphors)
                "$opener $batsman ne $bowler ki gend par do run churaye! $metaphor Score $currentScore."
            }
            ball.runs == 1 -> {
                val metaphor = pickFresh(singlesMetaphors)
                "$batsman ne ek run liya. $metaphor Score $currentScore."
            }
            ball.extraType?.isNotEmpty() == true && ball.extraType != "None" -> {
                val metaphor = pickFresh(extrasMetaphors)
                "$opener $bowler ki taraf se $metaphor Score $currentScore!"
            }
            else -> {
                val metaphor = pickFresh(dotsMetaphors)
                "$bowler ki kassi hui delivery! $metaphor Score $currentScore par kayam."
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
