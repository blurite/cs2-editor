package dawn.cs2.unscramble

object UnscrambleUtils {
    
    @JvmStatic
    fun read(scramble: HashMap<Int, Int>, unscramble: HashMap<Int, Int>) {
        val text = javaClass.getResource("/cs2/opcode/unscramble/osrs.txt").readText()
        for (i in text.lines()) {
            val opcodes = i.split(" ")
            if (opcodes.size < 2 || opcodes[0] == "?" || opcodes[0].startsWith("#")) {
                continue
            }
            val master = opcodes[0].toInt()
            val scrambled = opcodes[1].toInt()
            if (scramble.put(master, scrambled) != null) {
                // throw RuntimeException("Bad config, duplicate scramble $master ->  ...")
            }
            if (unscramble.put(scrambled, master) != null) {
                // throw RuntimeException("Bad config, duplicate scramble ... -> $scrambled")
            }
        }
    }
}
