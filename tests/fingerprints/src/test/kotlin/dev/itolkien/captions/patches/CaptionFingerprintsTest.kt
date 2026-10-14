package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
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

    private fun call(name: String, receiver: Int = 1, range: Boolean = false): Instruction {
        val reference = ImmutableMethodReference(owner, name, emptyList(), track)
        return if (range) ImmutableInstruction3rc(Opcode.INVOKE_DIRECT_RANGE, receiver, 1, reference)
        else ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 1, receiver, 0, 0, 0, 0, reference)
    }

    private fun nativeDefault(name: String) = method(name, instructions = listOf(
        ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, field),
        ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0,
            ImmutableMethodReference("Landroid/view/accessibility/CaptioningManager;", "getLocale", emptyList(), "Ljava/util/Locale;")),
        ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0,
            ImmutableMethodReference("Landroid/view/accessibility/CaptioningManager;", "isEnabled", emptyList(), "Z")),
        ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
        ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))

    private fun wrapper(callee: String, receiver: Int = 1, range: Boolean = false) = method("wrapper", instructions = listOf(
        ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 1, field), call(callee, receiver, range),
        ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
        ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))

    private fun initializer(callee: String, receiver: Int = 1): Method =
        ImmutableMethod(owner, "modelReady", emptyList(), "V", public, emptySet(), emptySet(),
            ImmutableMethodImplementation(2, listOf(
                ImmutableInstruction22c(Opcode.IPUT_OBJECT, 0, 1, field), call(callee, receiver),
                ImmutableInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList()))

    @Test fun `model ready helper is distinguished from model reading wrapper`() {
        for (range in listOf(false, true)) {
            val helper = nativeDefault("renamedHelper")
            val wrapper = wrapper(helper.name, range = range)
            val initializer = initializer(helper.name)
            for (methods in listOf(listOf(helper, wrapper, initializer), listOf(initializer, wrapper, helper)))
                assertEquals(helper, resolveDefaultCaptionSelector(methods, owner, track, model).method)
        }
    }

    @Test fun `wrapper relation without model ready evidence remains ambiguous`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(nativeDefault("helper"), wrapper("helper")), owner, track, model)
        }
    }

    @Test fun `calls on another receiver do not prove a wrapper relation`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(nativeDefault("helper"), wrapper("helper", receiver = 0),
                initializer("helper")), owner, track, model)
        }
    }

    @Test fun `native preference calls alone do not override ambiguity`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(nativeDefault("helper"), method("other"),
                initializer("helper")), owner, track, model)
        }
    }

    @Test fun `wrapper and initializer without Android preference evidence remain ambiguous`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(method("helper"), wrapper("helper"),
                initializer("helper")), owner, track, model)
        }
    }

    @Test fun `initializer call on another receiver does not prove lifecycle relationship`() {
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(nativeDefault("helper"), wrapper("helper"),
                initializer("helper", receiver = 0)), owner, track, model)
        }
    }

    @Test fun `reciprocal helper and wrapper calls remain ambiguous`() {
        val helper = nativeDefault("helper")
        val recursive = method("helper", instructions = helper.implementation!!.instructions.toList().dropLast(1) +
            call("wrapper") + ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0))
        assertThrows(IllegalStateException::class.java) {
            resolveDefaultCaptionSelector(listOf(recursive, wrapper("helper"),
                initializer("helper")), owner, track, model)
        }
    }
}
