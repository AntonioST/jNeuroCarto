package io.ast.jneurocarto.javafx.script;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenizeTest {

    @Test
    public void purePyNone() {
        assertEquals(
          PyValue.None,
          new Tokenize("None").parseValue()
        );
        assertEquals(
          PyValue.None,
          new Tokenize("").parseValue()
        );
    }

    @Test
    public void purePyInt() {
        assertEquals(
          new PyValue.PyInt(123),
          new Tokenize("123").parseValue()
        );
    }

    @Test
    public void purePyFloat() {
        assertEquals(
          new PyValue.PyFloat(123.3),
          new Tokenize("123.3").parseValue()
        );
    }

    @Test
    public void purePyStr() {
        assertEquals(
          new PyValue.PyStr("123"),
          new Tokenize("'123'").parseValue()
        );
    }

    @Test
    public void purePyList() {
        assertEquals(
          PyValue.EMPTY_LIST,
          new Tokenize("[]").parseValue()
        );
        assertThrows(IllegalArgumentException.class, () -> {
            new Tokenize("[,]").parseValue();
        });
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123)),
          new Tokenize("[123]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123)),
          new Tokenize("[123,]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("[123, 321]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("[123, 321,]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123), new PyValue.PyStr("321")),
          new Tokenize("[123, '321']").parseValue()
        );
    }

    @Test
    public void purePyTuple() {
        assertEquals(
          PyValue.EMPTY_TUPLE,
          new Tokenize("()").parseValue()
        );
        assertEquals(
          new PyValue.PyInt(123),
          new Tokenize("(123)").parseValue()
        );
        assertEquals(
          new PyValue.PyTuple1(new PyValue.PyInt(123)),
          new Tokenize("(123,)").parseValue()
        );
        assertEquals(
          new PyValue.PyTuple2(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("(123, 321)").parseValue()
        );
        assertEquals(
          new PyValue.PyTuple2(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("(123, 321,)").parseValue()
        );
        assertEquals(
          new PyValue.PyTuple2(new PyValue.PyInt(123), new PyValue.PyStr("321")),
          new Tokenize("(123, '321')").parseValue()
        );
        assertEquals(
          new PyValue.PyTuple3(new PyValue.PyInt(1), new PyValue.PyInt(2), new PyValue.PyInt(3)),
          new Tokenize("(1,2,3)").parseValue()
        );
        assertEquals(
          new PyValue.PyTupleN(List.of(
            new PyValue.PyInt(1), new PyValue.PyInt(2), new PyValue.PyInt(3), new PyValue.PyInt(4)
          )),
          new Tokenize("(1,2,3,4)").parseValue()
        );
    }

    @Test
    public void purePyDict() {
        assertEquals(
          PyValue.EMPTY_DICT,
          new Tokenize("{}").parseValue()
        );
        assertEquals(
          new PyValue.PyDict(List.of("1"), List.of(new PyValue.PyInt(1))),
          new Tokenize("{1:1}").parseValue()
        );
        assertEquals(
          new PyValue.PyDict(List.of("1"), List.of(new PyValue.PyInt(1))),
          new Tokenize("{1:1,}").parseValue()
        );
        assertEquals(
          new PyValue.PyDict(List.of("1", "2"),
            List.of(PyValue.None, PyValue.None)),
          new Tokenize("{1,2}").parseValue()
        );
        assertThrows(RuntimeException.class, () -> {
            new Tokenize("{1,1}").parseValue();
        });
    }

    @Test
    public void combine() {
        assertEquals(
          new PyValue.PyDict(List.of("a"),
            List.of(new PyValue.PyList(new PyValue.PyTuple1(new PyValue.PyInt(123))))),
          new Tokenize("{a: [(123,)]}").parseValue()
        );
    }
}
