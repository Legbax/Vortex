package com.lancelot

import com.lancelot.utils.ValidationUtils
import java.util.UUID
import java.util.Random

object SpoofingUtils {

    // === TAC MAP POR MODELO (Versión A - Recomendada) ===
    private val TAC_MAP: Map<String, List<String>> = mapOf(
        // Google Pixel
        "Pixel 5" to listOf("35674910", "35824005", "35935107"),
        "Pixel 4a" to listOf("35674910", "35824005"),
        "Pixel 4" to listOf("35674910", "35824006"),
        "Pixel 3a" to listOf("35328510", "35328511"),
        "Pixel 3" to listOf("35328510"),

        // Samsung Galaxy
        "Galaxy S20" to listOf("35271311", "35361311", "35449209"),
        "Galaxy A52" to listOf("35563409", "35563410"),
        "Galaxy A72" to listOf("35563409", "35563410"),
        "Galaxy Note 20" to listOf("35271311", "35361311"),
        "Galaxy Z Flip" to listOf("35449209"),
        "Galaxy S10" to listOf("35271311"),

        // Xiaomi / Redmi / Poco (Lancelot focus)
        "Redmi Note 9 Pro" to listOf("86413405", "86413404", "86413403"),
        "Redmi Note 10" to listOf("86413405", "86413404"),
        "Mi 10" to listOf("86413405", "86712345"),
        "Mi 11" to listOf("86413405", "86712346"),
        "Mi 10T" to listOf("86413405"),
        "Poco X3 NFC" to listOf("86413405", "86413404"),

        // OnePlus
        "OnePlus 8" to listOf("35824005", "35912345"),
        "OnePlus 8T" to listOf("35824005", "35912345"),
        "OnePlus Nord" to listOf("35824005"),
        "OnePlus 9" to listOf("35824005", "35912346"),
        "OnePlus 7 Pro" to listOf("35824005"),
        "OnePlus Nord N10" to listOf("35824005"),

        // Sony Xperia
        "Xperia 5 II" to listOf("35123456", "35234567"),
        "Xperia 10 II" to listOf("35123456"),
        "Xperia 1 II" to listOf("35123456"),
        "Xperia 5" to listOf("35123456"),
        "Xperia 10 III" to listOf("35123456"),
        "Xperia XZ2" to listOf("35123456")
    )

    // Optimized paths to hide (Set for O(1) access)
    private val SENSITIVE_PATHS = setOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su",
        "/data/local/su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su",
        "/su/bin/su", "/system/xbin/daemonsu",
        "/system/app/Superuser.apk", "/system/app/SuperSU.apk"
    )

    // Sensitive commands to block
    private val SENSITIVE_COMMANDS = setOf(
        "su", "which su", "mount", "getprop ro.secure"
    )

    fun isSensitivePath(path: String): Boolean {
        if (path.isEmpty() || path[0] != '/') return false
        if (path.contains("com.lancelot")) return false
        if (SENSITIVE_PATHS.contains(path)) return true

        if (path.length > 6) {
            if (path.startsWith("/data/adb/modules")) return true
            if (path.contains("magisk", true) ||
                path.contains("xposed", true) ||
                path.contains("lsposed", true)) {
                return true
            }
        }
        return false
    }

    fun isSensitiveCommand(command: List<String>): Boolean {
        if (command.isEmpty()) return false
        val fullCmd = command.joinToString(" ").lowercase()

        if (SENSITIVE_COMMANDS.contains(fullCmd)) return true
        if (fullCmd.startsWith("su ")) return true

        if (fullCmd.contains("magisk") ||
            fullCmd.contains("xposed") ||
            fullCmd.contains("lsposed") ||
            fullCmd.contains("busybox")) {
            return true
        }

        if (fullCmd.contains("which su") ||
            fullCmd.contains("ls /sbin") ||
            fullCmd.contains("ls /data/adb")) {
            return true
        }

        for (part in command) {
            if (part.startsWith("/") && isSensitivePath(part)) {
                return true
            }
        }
        return false
    }

    /** Genera IMEI coherente con el modelo seleccionado */
    fun generateValidImei(modelName: String): String {
        val tacList = TAC_MAP.entries.firstOrNull {
            modelName.contains(it.key, ignoreCase = true)
        }?.value ?: listOf("86413405") // fallback seguro para Lancelot

        val tac = tacList.random()
        val serial = (0..999999).random().toString().padStart(6, '0')
        val base = tac + serial
        val check = ValidationUtils.luhnChecksum(base)
        return base + check
    }

    fun generateValidImei(): String {
        // Fallback overload for compatibility if needed, using default
        return generateValidImei("Redmi 9")
    }

    fun generateValidIccid(mccMnc: String): String {
        val mnc = if (mccMnc.length >= 6) mccMnc.substring(3) else "260"
        val issuer = (10..99).random().toString()
        val prefixPart = "891$mnc$issuer"

        val accountLen = 18 - prefixPart.length
        val account = (1..accountLen).map { (0..9).random() }.joinToString("")
        val base = prefixPart + account
        val check = ValidationUtils.luhnChecksum(base)
        return base + check
    }

    fun generateValidImsi(mccMnc: String): String {
        val needed = 15 - mccMnc.length

        // Primeros 2-3 dígitos del MSIN suelen ser rangos del operador (HLR)
        val operatorRange = when (mccMnc) {
            "310260" -> (60..69).random()  // T-Mobile HLR ranges
            "310410" -> (40..49).random()  // AT&T
            "310012" -> (10..19).random()  // Verizon
            else -> (10..99).random()
        }

        // El MSIN total suele ser de 9 o 10 dígitos. MCC(3) + MNC(2/3) + MSIN = 15
        // Si needed es 9 (MNC 3 digitos), operatorRange (2) + 7 random
        val randomPartLen = needed - operatorRange.toString().length
        val randomPart = (1..randomPartLen).map { (0..9).random() }.joinToString("")

        return mccMnc + operatorRange.toString() + randomPart
    }

    fun generateRandomId(len: Int) = (1..len).map { "0123456789abcdef".random() }.joinToString("")

    fun generateRandomGaid(): String {
        return "${generateRandomId(8)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(12)}"
    }

    fun generateRandomSerial(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..12).map { chars.random() }.joinToString("")
    }

    fun generateRandomMac(): String {
        val bytes = ByteArray(6)
        Random().nextBytes(bytes)
        bytes[0] = (bytes[0].toInt() and 0xFC or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    fun generatePhoneNumber(npaList: List<String>): String {
        val npa = if (npaList.isNotEmpty()) npaList.random() else "202"

        var nxx = (200..999).random()
        // Bloquear rangos problemáticos (555, 9XX reservados, X11)
        while (nxx == 555 ||
               nxx in 950..999 ||
               (nxx / 100 == 9 && nxx % 10 == 1)) {
            nxx = (200..999).random()
        }

        val subscriber = (0..9999).random().toString().padStart(4, '0')
        return "+1$npa$nxx$subscriber"
    }

    fun generateRealisticGmail(): String {
        val firstNames = listOf(
            "sofia","sof","camila","cami","valentina","val","isabella","bella","lucia","luci",
            "daniela","dani","valeria","vale","gabriela","gaby","mariana","mari","catalina","cata",
            "ximena","xim","victoria","vicky","natalia","nati","alejandra","ale","fernanda","fer",
            "paulina","pauli","renata","re","emilia","emi","mia","sara","sarita","laura","laur",
            "ana","anita","maria","mary","carmen","carme","rosa","rosy","luna","lunita","aurora","rory",
            "sophia","olivia","emma","ava","charlotte","amelia","harper","evelyn","abigail","ella",
            "scarlett","grace","chloe","lily","nora","hazel","zoey","riley","layla","violet",
            "nova","ivy","stella","maya","penelope","everly","willow","eleanor","hannah","addison",
            "aubrey","brooklyn","claire","savannah","skylar","genesis","madison","isla","aaliyah",
            "jasmine","jas","ruby","alexa","lexi","brianna","bri","kaylee","kayla","megan","meg",
            "sydney","syd","rachel","rach","nicole","nikki","vanessa","nessa","andrea","drea",
            "veronica","vero","monica","mon","patricia","patty","elena","eli","silvia","sil",
            "beatriz","bea","carolina","caro","adriana","adri","paola","pao","tamara","tami"
        )

        val lastNames = listOf(
            "garcia","garci","rodriguez","rodri","lopez","lope","martinez","marti","hernandez","hernan",
            "gonzalez","gonza","smith","smit","johnson","johns","williams","will","brown","brow",
            "jones","jone","miller","mill","davis","davi","wilson","wils","moore","moor","taylor","tay",
            "anderson","thomas","jackson","white","harris","martin","thompson","garcia","martinez",
            "robinson","clark","rodriguez","lewis","lee","walker","hall","allen","young","hernandez",
            "king","wright","lopez","scott","green","adams","baker","gonzalez","nelson","carter",
            "mitchell","perez","roberts","turner","phillips","campbell","parker","evans","edwards",
            "collins","stewart","sanchez","morris","rogers","reed","cook","morgan","bell","murphy",
            "bailey","rivera","cooper","richardson","cox","howard","ward","torres","peterson","gray",
            "ramirez","james","watson","brooks","kelly","sanders","price","bennett","wood","barnes",
            "ross","henderson","coleman","jenkins","perry","powell","long","patterson","hughes",
            "flores","washington","butler","simmons","foster","gonzales","bryant","alexander","russell",
            "griffin","diaz","hayes","myers","ford","hamilton","graham","sullivan","wallace","woods",
            "cole","west","jordan","owens","reynolds","fisher","ellis","harrison","gibson","mcdonald",
            "cruz","marshall","ortiz","gomez","murray","freeman","wells","webb","simpson","stevens",
            "tucker","porter","hunter","hicks","crawford","henry","boyd","mason","moreno","kennedy",
            "warren","dixon","ramos","reyes","burns","gordon","shaw","holmes","rice","robertson",
            "hunt","black","daniels","palmer","mills","nichols","grant","knight","ferguson","rose",
            "stone","hawkins","dunn","perkins","hudson","spencer","gardner","stephens","payne","pierce",
            "berry","matthews","arnold","wagner","watkins","olson","carroll","duncan","snyder","hart",
            "cunningham","bradley","lane","andrews","ruiz","harper","fox","riley","armstrong","carpenter",
            "weaver","greene","lawrence","elliott","chavez","sims","austin","peters","kelley","franklin",
            "lawson","fields","gutierrez","ryan","schmidt","carr","vasquez","castillo","wheeler","chapman",
            "oliver","montgomery","richards","williamson","johnston","banks","meyer","bishop","mccoy",
            "howell","alvarez","morrison","hansen","fernandez","garza","harvey","little","burton","stanley",
            "nguyen","george","jacobs","reid","kim","fuller","lynch","dean","gilbert","garrett","romero",
            "welch","larson","frazier","burke","hanson","day","mendoza","moreno","bowman","medina","fowler",
            "brewer","hoffman","carlson","silva","pearson","holland","douglas","fleming","jensen","vargas",
            "byrd","davidson","hopkins","may","terry","herrera","wade","soto","walters","curtis","neal",
            "caldwell","lowe","jennings","barnett","graves","jimenez","horton","shelton","barrett","obrien",
            "castro","sutton","gregory","mckinney","lucas","miles","craig","rodriquez","chambers","holt",
            "lambert","fletcher","watts","bates","hale","rhodes","pena","beck","newman","haynes","mcdaniel",
            "mendez","bush","vaughn","phelps","mccormick","baldwin","kerr","murray","cordova","gibbs"
        )

        val first = firstNames.random()
        val last = lastNames.random()
        val separator = listOf("", ".", "_").random()

        var email = "${first}${separator}${last}"

        // Garantizamos mínimo 9 caracteres
        if (email.length < 9) {
            val needed = 9 - email.length
            val extra = (0..(Math.pow(10.0, needed.toDouble()) - 1).toInt())
                .random()
                .toString()
                .padStart(needed, '0')
            email += extra
        }

        // 55% probabilidad de agregar números extras
        if ((0..99).random() < 55) {
            val extraLen = (1..4).random()
            val number = (0..(Math.pow(10.0, extraLen.toDouble()) - 1).toInt())
                .random()
                .toString()
            email += number
        }

        return "$email@gmail.com"
    }
}
