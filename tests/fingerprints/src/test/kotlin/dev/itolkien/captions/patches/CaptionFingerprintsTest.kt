package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Synthetic bytecode shapes, not a claim to contain any YouTube APK's code. */
class CaptionFingerprintsTest {
    private val owner = "Lfixture/Manager;"
    private val track = "Lfixture/Track;"
    private val model = "Lfixture/Model;"
    private val field = ImmutableFieldReference(owner, "captions", model)
    private val public = AccessFlags.PUBLIC.value

    private fun method(name: String, flags: Int = public, registers: Int = 2,
                       instructions: List<Instruction> = listOf(
                           ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, field),
                           ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                           ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                       )): Method = ImmutableMethod(owner, name, emptyList(), track, flags,
        emptySet(), emptySet(), ImmutableMethodImplementation(registers, instructions, emptyList(), emptyList()))

    @Test fun `single legacy model reader is retained`() {
        val original = method("a")
        val found = resolveDefaultCaptionSelector(listOf(original), owner, track, model)
        assertEquals(original, found.method)
        assertEquals(field, found.modelField)
    }

    @Test fun `extra track getter does not make default selector ambiguous`() {
        val actual = method("b")
        val getter = method("a", instructions = listOf(
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1,
                ImmutableFieldReference(owner, "current", track)),
            ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))
        for (candidates in listOf(listOf(actual, getter), listOf(getter, actual)))
            assertEquals(actual, resolveDefaultCaptionSelector(candidates, owner, track, model).method)
    }

    @Test fun `static lookalike is ignored`() {
        assertEquals("b", resolveDefaultCaptionSelector(
            listOf(method("a", public or AccessFlags.STATIC.value), method("b")), owner, track, model).method.name)
    }

    @Test fun `writing a model field is not reading it`() {
        val writer = method("a", instructions = listOf(
            ImmutableInstruction22c(Opcode.IPUT_OBJECT, 0, 1, field),
            ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
            ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))
        assertEquals("b", resolveDefaultCaptionSelector(listOf(writer, method("b")), owner, track, model).method.name)
    }

    @Test fun `field on another receiver is not manager state`() {
        val other = method("a", registers = 3) // p0 is v2, not v1
        assertEquals("b", resolveDefaultCaptionSelector(listOf(other, method("b")), owner, track, model).method.name)
    }

    @Test fun `repeated reads of same field are not ambiguous`() {
        val repeated = method("a", instructions = listOf(
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, field),
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, field),
            ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))
        assertEquals(field, resolveDefaultCaptionSelector(listOf(repeated), owner, track, model).modelField)
    }

    @Test fun `two model readers fail with candidate descriptors`() {
        val error = assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(method("a"), method("b")), owner, track, model)
        }
        assertTrue(error.message!!.contains("$owner->a()$track"), error.message)
        assertTrue(error.message!!.contains("$owner->b()$track"), error.message)
        assertTrue(error.message!!.contains("found 2"), error.message)
    }

    @Test fun `no model reader fails instead of accepting lone getter`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(method("a", instructions = listOf(
                ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))), owner, track, model)
        }
    }

    @Test fun `multiple model fields fail instead of picking first`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(method("a", instructions = listOf(
                ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, field),
                ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, ImmutableFieldReference(owner, "other", model)),
                ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))), owner, track, model)
        }
    }

    @Test fun `no scratch register fails before injection`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(method("a", registers = 1, instructions = listOf(
                ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 0, field),
                ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))), owner, track, model)
        }
    }

    private fun listGetter(name: String, definingClass: String = model, flags: Int = public): Method =
        ImmutableMethod(definingClass, name, emptyList(), "Ljava/util/List;", flags,
            emptySet(), emptySet(), ImmutableMethodImplementation(2, listOf(
                ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)), emptyList(), emptyList()))

    @Test fun `direct list anchor survives extra list getters`() {
        val direct = listGetter("b")
        val translated = listGetter("c")
        assertEquals(direct, resolveSourceCaptionGetter(
            listOf(listGetter("a"), translated, direct), model, direct, translated))
    }

    @Test fun `direct anchor on different owner fails`() {
        assertThrows(IllegalStateException::class.java) {
            resolveSourceCaptionGetter(listOf(listGetter("a")), model,
                listGetter("a", "Lfixture/Other;"), listGetter("b"))
        }
    }

    @Test fun `inaccessible or static direct list getter fails`() {
        for (flags in listOf(AccessFlags.PRIVATE.value, public or AccessFlags.STATIC.value)) {
            val direct = listGetter("a", flags = flags)
            assertThrows(IllegalStateException::class.java) {
                resolveSourceCaptionGetter(listOf(direct), model, direct, listGetter("b"))
            }
        }
    }
}
