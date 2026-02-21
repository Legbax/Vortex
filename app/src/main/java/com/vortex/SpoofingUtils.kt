package com.vortex

import java.util.Random
import java.util.UUID

object SpoofingUtils {

    // TACs mapeados POR FABRICANTE para correlacionar con el perfil activo.
    private val TACS_BY_BRAND = mapOf(
        "Xiaomi"   to listOf("86413404", "86413405", "35271311", "35361311", "86814904"),
        "POCO"     to listOf("86814904", "86814905", "35847611", "35847612"),
        "Redmi"    to listOf("86413404", "86413405", "35271311", "35271312"),
        "samsung"  to listOf("35449209", "35449210", "35355610", "35735110", "35735111"),
        "Google"   to listOf("35674910", "35674911", "35308010", "35308011"),
        "OnePlus"  to listOf("86882504", "86882505", "35438210", "35438211"),
        "motorola" to listOf("35617710", "35617711", "35327510", "35327511"),
        "Nokia"    to listOf("35720210", "35720211", "35489310"),
        "realme"   to listOf("86828804", "86828805", "35388910"),
        "vivo"     to listOf("86979604", "86979605", "35503210"),
        "OPPO"     to listOf("86885004", "86885005", "35604210"),
        "asus"     to listOf("35851710", "35851711", "35325010"),
        "default"  to listOf("35271311", "35449209", "35674910")
    )

    private val OUIS = listOf(
        byteArrayOf(0x40, 0x4E, 0x36),  // Qualcomm Atheros
        byteArrayOf(0x60, 0x57, 0x18),  // MediaTek / Ralink
        byteArrayOf(0x8C.toByte(), 0xDE.toByte(), 0x52),  // Realtek
        byteArrayOf(0xD4.toByte(), 0x61, 0x9D.toByte()),  // Broadcom
        byteArrayOf(0xF0.toByte(), 0x1F, 0xAF.toByte()),  // Qualcomm (nuevo)
        byteArrayOf(0xA4.toByte(), 0xC3.toByte(), 0xF0.toByte()),  // Google
        byteArrayOf(0x00, 0x23, 0x76),  // Intel
        byteArrayOf(0x00, 0x26, 0x86.toByte()),  // Cisco-Linksys
        byteArrayOf(0xD4.toByte(), 0xBE.toByte(), 0xD9.toByte()),  // Samsung
        byteArrayOf(0xAC.toByte(), 0x37, 0x43)   // Huawei/MediaTek
    )

    /**
     * Genera un IMEI válido correlacionado con la marca del perfil activo.
     */
    fun generateValidImei(profileName: String = "", seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val brand = DeviceData.DEVICE_FINGERPRINTS[profileName]?.brand ?: ""
        val tacList = TACS_BY_BRAND[brand] ?: TACS_BY_BRAND["default"]!!
        val tac = if (seed != null) tacList[Math.abs(rng.nextInt()) % tacList.size] else tacList.random()

        val serial = (1..6).map { rng.nextInt(10) }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }

    fun generateValidIccid(mccMnc: String, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val issuer = (10 + rng.nextInt(90)).toString()
        val prefix = "89$mccMnc$issuer"
        val needed = 18 - prefix.length
        val account = (1..needed.coerceAtLeast(1)).map { rng.nextInt(10) }.joinToString("")
        val base = prefix + account
        return base + luhnChecksum(base)
    }

    fun generateValidImsi(mccMnc: String, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        // [FIX D10] Primer dígito MSIN: 2-9 (evitar 0,1 reservados)
        val firstDigit = 2 + rng.nextInt(8)
        val rest = (1..8).map { rng.nextInt(10) }.joinToString("")
        return mccMnc + firstDigit.toString() + rest
    }

    fun isLuhnValid(number: String): Boolean {
        if (number.isEmpty() || !number.all { it.isDigit() }) return false
        var sum = 0; val len = number.length; val p = len % 2
        for (i in 0 until len) {
            var d = number[i].digitToInt()
            if (i % 2 == p) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        return sum % 10 == 0
    }

    private fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var d = number[i].digitToInt()
            if ((number.length - i + 1) % 2 == 0) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        return (10 - (sum % 10)) % 10
    }

    fun generateRandomId(len: Int, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val chars = "0123456789abcdef"
        return (1..len).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }

    fun generateRandomGaid(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        if (seed != null) {
            val mostSigBits = rng.nextLong()
            val leastSigBits = rng.nextLong()
            return UUID(mostSigBits, leastSigBits).toString()
        }
        return UUID.randomUUID().toString()
    }

    fun generateRandomSerial(brand: String = "", seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val alphaNum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        return when (brand.lowercase()) {
            "samsung" -> {
                val year = if (rng.nextBoolean()) "21" else "22"
                val month = (1 + rng.nextInt(12)).toString().padStart(2, '0')
                val suffix = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"[rng.nextInt(34)] }.joinToString("")
                "R${year}${month}${suffix}"
            }
            "google" -> {
                (1..14).map { alphaNum[rng.nextInt(alphaNum.length)] }.joinToString("")
            }
            else -> {
                val len = 8 + rng.nextInt(5)
                (1..len).map { alphaNum[rng.nextInt(alphaNum.length)] }.joinToString("")
            }
        }
    }

    fun generateRandomMac(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        // [FIX D13] Usar OUI real
        val oui = OUIS[rng.nextInt(OUIS.size)]
        val suffix = ByteArray(3)
        rng.nextBytes(suffix)
        val full = oui + suffix
        // No forzar bit 'locally administered' (0x02) para parecer hardware real
        return full.joinToString(":") { "%02X".format(it) }
    }

    fun generateRealisticGmail(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()

        // ── FIRST NAMES (332 total) ──────────────────────────────────────────────
        // 32 Original American Male
        // 100 New American (50M + 50F) | 100 New European (50M + 50F)
        // 100 New Latino  (50M + 50F)
        // Verified: zero duplicates across all 332 entries.
        val first = listOf(
            // ── Original American Male (32) ──────────────────────────────────────
            "james", "john", "robert", "michael", "william", "david", "joseph", "charles",
            "thomas", "daniel", "matthew", "anthony", "mark", "donald", "steven", "paul",
            "andrew", "joshua", "kenneth", "kevin", "brian", "george", "timothy", "ronald",
            "edward", "jason", "jeffrey", "ryan", "jacob", "gary", "nicholas", "eric",

            // ── New American Male (50) ────────────────────────────────────────────
            "christopher", "brandon", "justin", "tyler", "austin", "dylan", "aaron", "zachary",
            "adam", "patrick", "sean", "travis", "chad", "derek", "shawn", "cody", "hunter",
            "cole", "blake", "trevor", "jesse", "kyle", "ethan", "caleb", "luke", "noah",
            "mason", "logan", "liam", "owen", "connor", "eli", "ian", "alex", "marcus",
            "xavier", "nathaniel", "raymond", "carl", "jerome", "marvin", "phillip", "curtis",
            "floyd", "glen", "lloyd", "clifton", "wendell", "terrence", "warren",

            // ── New American Female (50) ──────────────────────────────────────────
            "jennifer", "jessica", "ashley", "amanda", "melissa", "sarah", "stephanie",
            "elizabeth", "lauren", "rachel", "hannah", "emily", "brittany", "megan",
            "samantha", "kayla", "courtney", "amber", "tiffany", "heather", "holly", "kelly",
            "lisa", "linda", "patricia", "barbara", "carol", "alice", "emma", "olivia",
            "sophia", "ava", "madison", "abigail", "ella", "grace", "lily", "chloe",
            "victoria", "aria", "riley", "nora", "penelope", "layla", "savannah", "audrey",
            "claire", "natalie", "leah", "anna",

            // ── New European Male (50) ────────────────────────────────────────────
            // French
            "pierre", "baptiste", "hugo", "theo", "maxime", "clement", "remi",
            // German
            "hans", "dieter", "franz", "stefan", "lukas", "felix", "moritz", "tobias",
            // Spanish (Spain)
            "javier", "pablo", "sergio",
            // Italian
            "marco", "luca", "matteo", "giovanni", "roberto", "stefano",
            // Scandinavian
            "lars", "sven", "bjorn", "niels",
            // Eastern European
            "nikolai", "dmitri", "alexei", "jan", "andrei", "vasile",
            // Various European
            "kristian", "arno", "bastian", "leon", "florian", "rupert", "gunther", "ernst",
            "oskar", "filip", "zoltan", "mirko", "branko",
            // British
            "harry", "oliver", "archie",

            // ── New European Female (50) ──────────────────────────────────────────
            // French
            "amelie", "juliette", "lea", "manon", "margot", "elise", "pauline",
            // German
            "marie", "lena", "katja", "sabine", "monika", "petra",
            // Spanish (Spain)
            "carmen", "pilar", "rosa", "isabel", "teresa",
            // Italian
            "valentina", "giulia", "federica", "chiara", "francesca",
            // Scandinavian
            "ingrid", "astrid", "freya", "maja", "sofie", "annika", "britta",
            // Eastern European / Russian
            "natasha", "olga", "tatiana", "svetlana", "irina",
            "katarzyna", "agnieszka", "magdalena", "marta", "zofia",
            // British / Irish
            "amelia", "isla", "poppy", "eleanor", "harriet", "florence",
            "maeve", "aoife", "siobhan", "brigid",

            // ── New Latino Male (50) ──────────────────────────────────────────────
            "jose", "juan", "luis", "pedro", "fernando", "jorge", "ricardo", "andres",
            "rafael", "mario", "victor", "oscar", "rodrigo", "guillermo", "raul", "alberto",
            "hector", "ernesto", "arturo", "ignacio", "enrique", "manuel", "nicolas",
            "sebastian", "mauricio", "gerardo", "armando", "aldo", "omar", "gabriel",
            "santiago", "mateo", "julian", "camilo", "fabian", "cesar", "edgar", "diego",
            "francisco", "miguel", "carlos", "antonio", "alejandro", "ivan", "felipe",
            "emilio", "ruben", "aurelio", "gonzalo", "rolando",

            // ── New Latino Female (50) ────────────────────────────────────────────
            "guadalupe", "maria", "luz", "esperanza", "yolanda", "graciela", "beatriz",
            "consuelo", "marisol", "lourdes", "rebeca", "alicia", "norma", "silvia",
            "delia", "blanca", "xiomara", "yasmin", "itzel", "citlali", "yareli", "brenda",
            "ariadna", "vanesa", "rocio", "veronica", "claudia", "adriana", "paola",
            "fernanda", "valeria", "daniela", "carolina", "natalia", "sofia", "mariana",
            "alejandra", "monica", "diana", "elena", "ana", "lucia", "josefina", "lorena",
            "miriam", "cecilia", "leticia", "gladys", "amparo", "piedad"
        )

        // ── LAST NAMES (532 total) ───────────────────────────────────────────────
        // 32 Original | 250 New American | 100 New European | 150 New Latino
        // Verified: zero duplicates across all 532 entries.
        val last = listOf(
            // ── Original (32) ────────────────────────────────────────────────────
            "smith", "johnson", "williams", "brown", "jones", "garcia", "miller", "davis",
            "wilson", "taylor", "anderson", "thomas", "jackson", "white", "harris", "martin",
            "thompson", "young", "robinson", "lewis", "walker", "allen", "hall", "wright",
            "scott", "green", "adams", "baker", "nelson", "carter", "mitchell", "perez",

            // ── New American (250) ────────────────────────────────────────────────
            "moore", "lee", "clark", "rodriguez", "martinez", "hernandez", "lopez", "gonzalez",
            "turner", "phillips", "campbell", "parker", "evans", "edwards", "collins", "stewart",
            "sanchez", "morris", "rogers", "reed", "cook", "morgan", "bell", "murphy", "bailey",
            "rivera", "cooper", "richardson", "cox", "howard", "ward", "torres", "peterson",
            "gray", "ramirez", "watson", "brooks", "kelly", "sanders", "price", "bennett",
            "wood", "barnes", "ross", "henderson", "coleman", "jenkins", "perry", "powell",
            "long", "patterson", "hughes", "flores", "washington", "butler", "simmons", "foster",
            "gonzales", "bryant", "alexander", "russell", "griffin", "diaz", "hayes", "myers",
            "ford", "hamilton", "graham", "sullivan", "wallace", "woods", "cole", "west",
            "jordan", "owens", "reynolds", "fisher", "ellis", "harrison", "gibson", "mcdonald",
            "cruz", "marshall", "ortiz", "gomez", "murray", "freeman", "wells", "webb",
            "simpson", "stevens", "tucker", "porter", "hunter", "hicks", "crawford", "henry",
            "boyd", "mason", "morales", "kennedy", "warren", "dixon", "ramos", "reyes",
            "burns", "gordon", "shaw", "holmes", "rice", "robertson", "hunt", "black",
            "daniels", "palmer", "mills", "nichols", "grant", "knight", "ferguson", "rose",
            "stone", "hawkins", "dunn", "perkins", "hudson", "spencer", "gardner", "stephens",
            "payne", "pierce", "berry", "matthews", "arnold", "wagner", "willis", "ray",
            "watkins", "olson", "carroll", "duncan", "snyder", "hart", "cunningham", "bradley",
            "lane", "andrews", "ruiz", "harper", "fox", "riley", "armstrong", "carpenter",
            "weaver", "elliott", "chavez", "sims", "austin", "peters", "kelley", "franklin",
            "lawson", "fields", "gutierrez", "pope", "bates", "horton", "sutton", "malone",
            "mccoy", "rodgers", "gross", "cross", "bowers", "barker", "chambers", "obrien",
            "walters", "aguilar", "cobb", "french", "kramer", "mccormick", "clarke", "becker",
            "hoffman", "medina", "fletcher", "guerrero", "holt", "glover", "moss", "christensen",
            "garrett", "wade", "cannon", "vargas", "sparks", "barrera", "mejia", "garza",
            "thornton", "valdez", "norris", "lamb", "stevenson", "ball", "bishop", "burnett",
            "barton", "swanson", "byrd", "moran", "little", "wilkins", "robbins", "gill",
            "vega", "gibbs", "frank", "pittman", "crane", "stafford", "mcbride", "golden",
            "acosta", "maxwell", "stark", "odom", "hubbard", "bonner", "gillespie", "mcintyre",
            "morse", "odonnell", "haley", "frazier", "mullins", "lowe", "romero",
            "patton", "estes", "mckinney", "phelps", "nolan", "dyer", "gallagher",
            "mclaughlin", "vance", "lindsey",

            // ── New European (100) ────────────────────────────────────────────────
            // German (20)
            "mueller", "schmidt", "schneider", "fischer", "weber", "schulz", "richter",
            "klein", "wolf", "schroeder", "neumann", "schwarz", "zimmermann", "braun",
            "kruger", "hartmann", "lange", "lehmann", "schmitt", "werner",
            // French (20)
            "bernard", "petit", "richard", "durand", "dubois", "moreau", "simon", "michel",
            "lefevre", "roux", "bertrand", "morel", "girard", "lambert", "fontaine",
            "chevalier", "blanchard", "colin", "renard", "leclerc",
            // Italian (20)
            "ferrari", "rossi", "esposito", "bianchi", "romano", "colombo", "ricci",
            "marino", "greco", "bruno", "conti", "mancini", "costa", "giordano", "rizzo",
            "lombardi", "barbieri", "ferrara", "santoro", "deluca",
            // Spanish — Spain (10)
            "fernandez", "alvarez", "jimenez", "alonso", "munoz", "ortega", "delgado",
            "navarro", "dominguez", "molina",
            // British / Irish (10)
            "oconnor", "doyle", "brennan", "reilly", "mckenzie", "fraser", "mclean",
            "mcgregor", "paterson", "dunne",
            // Dutch / Scandinavian (10)
            "dejong", "jansen", "bakker", "meijer", "visser", "lindqvist", "karlsson",
            "eriksson", "johansson", "larsen",
            // Eastern European (10)
            "kowalski", "nowak", "wisniewski", "wozniak", "kowalczyk", "novak", "kovac",
            "horvat", "jovanovic", "petrovic",

            // ── New Latino (150) ──────────────────────────────────────────────────
            "suarez", "espinoza", "guzman", "reina", "cardenas", "castro", "herrera",
            "rojas", "fuentes", "meza", "nunez", "pena", "varela", "salinas", "solis",
            "pacheco", "montes", "campos", "avila", "lara", "rios", "trevino", "cisneros",
            "montoya", "rivas", "zamorano", "ibarra", "arroyo", "mondragon", "tapia",
            "tellez", "vergara", "palomino", "nava", "mata", "leyva", "limon", "estrada",
            "lugo", "villa", "nieto", "pimentel", "rangel", "orozco", "trujillo", "serrano",
            "cabrera", "obregon", "caro", "ojeda", "villanueva", "palacios", "vela",
            "saavedra", "gallardo", "paredes", "benitez", "sandoval", "padilla", "zuniga",
            "bermudez", "centeno", "navarrete", "duarte", "soto", "cordoba", "cuadrado",
            "espino", "quintero", "morejon", "barraza", "bustamante", "cedillo", "contreras",
            "escamilla", "galan", "giron", "guerra", "herrero", "ledezma", "lozano",
            "maldonado", "mercado", "montiel", "negrete", "ochoa", "palma", "quezada",
            "rendon", "rosales", "saldana", "saucedo", "segura", "tamayo", "uribe", "vera",
            "villeda", "zarate", "bautista", "bravo", "calderon", "cantu", "carmona",
            "cedeno", "cervantes", "corona", "crespo", "escobedo", "fajardo", "gamboa",
            "granados", "jaime", "landeros", "macedo", "marquez", "meraz", "mireles",
            "montemayor", "mora", "naranjo", "noriega", "paramo", "pizarro", "plata",
            "quijano", "robledo", "salcedo", "saldivar", "serratos", "tirado", "urrutia",
            "vasquez", "zazueta", "aguirre", "alcantar", "andrade", "angulo", "aranda",
            "arellano", "arias", "arreola", "baeza", "becerra", "bello", "bernal",
            "castellanos", "covarrubias", "delacruz", "elizondo", "fragoso"
        )

        val f = first[rng.nextInt(first.size)]
        val l = last[rng.nextInt(last.size)]
        val sep = listOf("", ".", "_")[rng.nextInt(3)]
        val num = if (rng.nextBoolean()) (1 + rng.nextInt(9999)).toString() else ""
        return "$f$sep$l$num@gmail.com"
    }

    fun generatePhoneNumber(npaList: List<String>, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val npa = if (npaList.isNotEmpty()) npaList[rng.nextInt(npaList.size)] else "212"
        var nxx = 200 + rng.nextInt(800)
        if (nxx == 555) nxx = 556
        val sub = rng.nextInt(10000).toString().padStart(4, '0')
        return "+1$npa$nxx$sub"
    }

    // [FIX D3] SSID Realista
    fun generateRealisticSsid(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val prefixes = listOf(
            "NETGEAR", "Linksys", "xfinitywifi", "ATT", "Spectrum",
            "TP-Link", "ASUS", "Archer", "dlink", "Belkin", "MyHome"
        )
        val prefix = prefixes[rng.nextInt(prefixes.size)]
        val suffixType = rng.nextInt(3)
        val suffix = when (suffixType) {
            0    -> "_${1000 + rng.nextInt(9000)}"
            1    -> "-${generateRandomId(4, seed).uppercase()}"
            else -> (1 + rng.nextInt(9)).toString()
        }
        return "$prefix$suffix"
    }

    /**
     * Genera un mapa con TODOS los valores esperados para un perfil dado.
     * Usado por StatusFragment para verificar consistencia.
     * Se usa una semilla determinista basada en el nombre del perfil para
     * que siempre devuelva los mismos valores "ideales" para ese perfil.
     */
    fun generateAllForProfile(profileName: String, mccMnc: String = "310260"): Map<String, String> {
        val seed = (profileName.hashCode() + mccMnc.hashCode()).toLong()
        val fp = DeviceData.DEVICE_FINGERPRINTS[profileName]
        val brand = fp?.brand ?: ""

        val carrier = DeviceData.getUsCarriers().find { it.mccMnc == mccMnc }
        val npas = carrier?.npas ?: emptyList()

        return mapOf(
            "imei"           to generateValidImei(profileName, seed),
            "imei2"          to generateValidImei(profileName, seed + 1),
            "imsi"           to generateValidImsi(mccMnc, seed),
            "iccid"          to generateValidIccid(mccMnc, seed),
            "phone_number"   to generatePhoneNumber(npas, seed),
            "android_id"     to generateRandomId(16, seed),
            "ssaid_snapchat" to generateRandomId(16, seed + 2),
            "gaid"           to generateRandomGaid(seed),
            "gsf_id"         to generateRandomId(16, seed + 3),
            "media_drm_id"   to generateRandomId(32, seed),
            "serial"         to generateRandomSerial(brand, seed),
            "wifi_mac"       to generateRandomMac(seed),
            "bluetooth_mac"  to generateRandomMac(seed + 1),
            "gmail"          to generateRealisticGmail(seed),
            "wifi_ssid"      to generateRealisticSsid(seed),
            "wifi_bssid"     to generateRandomMac(seed + 2)
        )
    }
}
