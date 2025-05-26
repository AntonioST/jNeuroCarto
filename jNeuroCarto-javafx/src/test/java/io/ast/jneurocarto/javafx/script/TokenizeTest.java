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
    public void purePyEmptyList() {
        assertEquals(
          PyValue.EMPTY_LIST,
          new Tokenize("[]").parseValue()
        );
    }

    @Test
    public void purePyListWithWrongSyntax() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Tokenize("[,]").parseValue();
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Tokenize("[,,]").parseValue();
        });
    }

    @Test
    public void purePyList() {
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123)),
          new Tokenize("[123]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("[123, 321]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123), new PyValue.PyStr("321")),
          new Tokenize("[123, '321']").parseValue()
        );
    }

    @Test
    public void purePyListWithTailingComma() {
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123)),
          new Tokenize("[123,]").parseValue()
        );
        assertEquals(
          new PyValue.PyList(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("[123, 321,]").parseValue()
        );
    }

    @Test
    public void purePyEmptyTuple() {
        assertEquals(
          PyValue.EMPTY_TUPLE,
          new Tokenize("()").parseValue()
        );
    }

    @Test
    public void purePyTupleWithWrongSyntax() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Tokenize("(,)").parseValue();
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Tokenize("(,,)").parseValue();
        });
    }


    @Test
    public void purePyTupleButWithSingleElement() {
        assertEquals(
          new PyValue.PyInt(123),
          new Tokenize("(123)").parseValue()
        );
    }

    @Test
    public void purePyTuple() {
        assertEquals(
          new PyValue.PyTuple2(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("(123, 321)").parseValue()
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
    public void purePyTupleWithTailingComma() {
        assertEquals(
          new PyValue.PyTuple1(new PyValue.PyInt(123)),
          new Tokenize("(123,)").parseValue()
        );
        assertEquals(
          new PyValue.PyTuple2(new PyValue.PyInt(123), new PyValue.PyInt(321)),
          new Tokenize("(123, 321,)").parseValue()
        );
    }

    @Test
    public void purePyEmptyDict() {
        assertEquals(
          PyValue.EMPTY_DICT,
          new Tokenize("{}").parseValue()
        );
    }

    @Test
    public void purePyDict() {
        assertEquals(
          new PyValue.PyDict(List.of("1"), List.of(new PyValue.PyInt(1))),
          new Tokenize("{1:1}").parseValue()
        );
    }

    @Test
    public void purePyDictAsSet() {
        assertEquals(
          new PyValue.PyDict(List.of("1", "2"),
            List.of(PyValue.None, PyValue.None)),
          new Tokenize("{1,2}").parseValue()
        );
    }

    @Test
    public void purePyDictWithTailComma() {
        assertEquals(
          new PyValue.PyDict(List.of("1"), List.of(new PyValue.PyInt(1))),
          new Tokenize("{1:1,}").parseValue()
        );
    }

    @Test
    public void purePyDictWithDuplicateKey() {
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

    @Test
    public void structureInStr() {
        assertEquals(
          new PyValue.PyStr("[]"),
          new Tokenize("'[]'").parseValue()
        );
        assertEquals(
          new PyValue.PyStr("[()]"),
          new Tokenize("'[()]'").parseValue()
        );
    }

    @Test
    public void parseLine() {
        assertEquals(List.of(
            new PyValue.PyIndexParameter(0, new PyValue.PyInt(1)),
            new PyValue.PyIndexParameter(1, new PyValue.PyInt(2)),
            new PyValue.PyIndexParameter(2, new PyValue.PyInt(3))),
          new Tokenize("1,2,3").parse().tokens
        );
    }

    @Test
    public void parseLineWithNullValue() {
        assertEquals(
          List.of(
            new PyValue.PyIndexParameter(0, new PyValue.PyInt(1)),
            new PyValue.PyIndexParameter(1, null),
            new PyValue.PyIndexParameter(2, new PyValue.PyInt(3))),
          new Tokenize("1,,3").parse().tokens
        );
        assertEquals(
          List.of(
            new PyValue.PyIndexParameter(0, null),
            new PyValue.PyIndexParameter(1, null),
            new PyValue.PyIndexParameter(2, null)),
          new Tokenize(",, ").parse().tokens
        );
        assertEquals(
          List.of(
            new PyValue.PyIndexParameter(0, null),
            new PyValue.PyIndexParameter(1, null),
            new PyValue.PyIndexParameter(2, null)),
          new Tokenize(",,").parse().tokens
        );
    }

    @Test
    public void parseLineWithName() {
        assertEquals(List.of(
            new PyValue.PyIndexParameter(0, new PyValue.PyInt(1)),
            new PyValue.PyNamedParameter("a", new PyValue.PyInt(2)),
            new PyValue.PyNamedParameter("b", new PyValue.PyInt(3))),
          new Tokenize("1,a=2,b=3").parse().tokens
        );
    }

    @Test
    public void parseLineWithNameAndNullValue() {
        assertEquals(List.of(
            new PyValue.PyIndexParameter(0, new PyValue.PyInt(1)),
            new PyValue.PyNamedParameter("a", null),
            new PyValue.PyNamedParameter("b", new PyValue.PyInt(3))),
          new Tokenize("1,a=,b=3").parse().tokens
        );

        assertEquals(List.of(
            new PyValue.PyIndexParameter(0, new PyValue.PyInt(1)),
            new PyValue.PyNamedParameter("a", new PyValue.PyInt(2)),
            new PyValue.PyNamedParameter("b", null)),
          new Tokenize("1,a=2,b=").parse().tokens
        );
    }
}
