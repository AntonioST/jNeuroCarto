package io.ast.jneurocarto.javafx.script;

import java.util.List;

import org.junit.jupiter.api.Test;

import static io.ast.jneurocarto.javafx.script.PyValue.*;
import static org.junit.jupiter.api.Assertions.*;

public class TokenizeTest {

    @Test
    public void purePyNone() {
        assertEquals(
          None,
          new Tokenize("None").parseValue()
        );
        assertEquals(
          None,
          new Tokenize("").parseValue()
        );
    }

    @Test
    public void purePyInt() {
        assertEquals(
          PyInt.of(123),
          new Tokenize("123").parseValue()
        );
    }

    @Test
    public void purePyFloat() {
        assertEquals(
          PyFloat.of(123.3),
          new Tokenize("123.3").parseValue()
        );
    }

    @Test
    public void purePyStr() {
        assertEquals(
          PyStr.of("123"),
          new Tokenize("'123'").parseValue()
        );
        assertEquals(
          PyStr.of("a"),
          new Tokenize("'a'").parseValue()
        );
    }

    @Test
    public void purePySymbol() {
        assertEquals(
          new PyToken("a"),
          new Tokenize("a").parseValue()
        );
    }

    @Test
    public void purePyEmptyList() {
        assertEquals(
          EMPTY_LIST,
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
        assertEquals(
          new PyToken("[,,"),
          new Tokenize("[,,").parseValue()
        );
    }

    @Test
    public void purePyList() {
        assertEquals(
          PyList.of(PyInt.of(123)),
          new Tokenize("[123]").parseValue()
        );
        assertEquals(
          PyList.of(PyInt.of(123), PyInt.of(321)),
          new Tokenize("[123, 321]").parseValue()
        );
        assertEquals(
          PyList.of(PyInt.of(123), PyStr.of("321")),
          new Tokenize("[123, '321']").parseValue()
        );
    }

    @Test
    public void purePyListWithTailingComma() {
        assertEquals(
          PyList.of(PyInt.of(123)),
          new Tokenize("[123,]").parseValue()
        );
        assertEquals(
          PyList.of(PyInt.of(123), PyInt.of(321)),
          new Tokenize("[123, 321,]").parseValue()
        );
    }

    @Test
    public void purePyEmptyTuple() {
        assertEquals(
          EMPTY_TUPLE,
          new Tokenize("()").parseValue()
        );
    }

    @Test
    public void purePyTupleEquals() {
        assertEquals(
          PyTuple.of(PyInt.of(1)),
          PyTuple.of(List.of(PyInt.of(1)))
        );
        assertEquals(
          PyTuple.of(PyInt.of(1)),
          new PyTupleN(List.of(PyInt.of(1)))
        );
        assertEquals(
          PyTuple.of(PyInt.of(1), PyInt.of(2)),
          PyTuple.of(List.of(PyInt.of(1), PyInt.of(2)))
        );
        assertEquals(
          PyTuple.of(PyInt.of(1), PyInt.of(2)),
          new PyTupleN(List.of(PyInt.of(1), PyInt.of(2)))
        );
        assertEquals(
          new PyTupleN(List.of(PyInt.of(1), PyInt.of(2))),
          PyTuple.of(PyInt.of(1), PyInt.of(2))
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
        assertEquals(
          new PyToken("(,,"),
          new Tokenize("(,,").parseValue()
        );
    }

    @Test
    public void purePyTupleButWithSingleElement() {
        assertEquals(
          PyInt.of(123),
          new Tokenize("(123)").parseValue()
        );
    }

    @Test
    public void purePyTuple() {
        assertEquals(
          PyTuple.of(PyInt.of(123), PyInt.of(321)),
          new Tokenize("(123, 321)").parseValue()
        );
        assertEquals(
          PyTuple.of(PyInt.of(123), PyStr.of("321")),
          new Tokenize("(123, '321')").parseValue()
        );
        assertEquals(
          PyTuple.of(PyInt.of(1), PyInt.of(2), PyInt.of(3)),
          new Tokenize("(1,2,3)").parseValue()
        );
        assertEquals(
          PyTuple.of(List.of(
            PyInt.of(1), PyInt.of(2), PyInt.of(3), PyInt.of(4)
          )),
          new Tokenize("(1,2,3,4)").parseValue()
        );
    }


    @Test
    public void purePyTupleWithTailingComma() {
        assertEquals(
          PyTuple.of(PyInt.of(123)),
          new Tokenize("(123,)").parseValue()
        );
        assertEquals(
          PyTuple.of(PyInt.of(123), PyInt.of(321)),
          new Tokenize("(123, 321,)").parseValue()
        );
    }

    @Test
    public void purePyEmptyDict() {
        assertEquals(
          EMPTY_DICT,
          new Tokenize("{}").parseValue()
        );
    }

    @Test
    public void prePyDictEquals() {
        var sd = PyDict.of("1", PyInt.of(1));
        var gd = PyDict.of(PyStr.of("1"), PyInt.of(1));
        assertNotSame(sd, gd);
        assertInstanceOf(PyStrDict.class, sd);
        assertInstanceOf(PyGeneralDict.class, gd);
        assertTrue(sd.equals(gd));
        assertTrue(gd.equals(sd));
    }

    @Test
    public void purePyDict() {
        assertEquals(
          PyDict.of(PyInt.of(1), PyInt.of(1)),
          new Tokenize("{1:1}").parseValue()
        );
    }

    @Test
    public void purePyDictTokenAsStrKey() {
        assertEquals(
          new PyStrDict(List.of("a"), List.of(PyInt.of(1))),
          new Tokenize("{a:1}").parseValue()
        );
    }

    @Test
    public void purePyDictStrAsKey() {
        assertEquals(
          PyDict.of("a", PyInt.of(1)),
          new Tokenize("{'a':1}").parseValue()
        );
    }

    @Test
    public void purePyDictIntAsKey() {
        assertEquals(
          PyDict.of(PyInt.of(1), PyInt.of(1)),
          new Tokenize("{1:1}").parseValue()
        );
    }

    @Test
    public void purePyDictFloatAsKey() {
        assertEquals(
          PyDict.of(PyFloat.of(1.23), PyInt.of(1)),
          new Tokenize("{1.23:1}").parseValue()
        );
    }

    @Test
    public void purePyDictTupleAsKey() {
        assertEquals(
          PyDict.of(PyTuple.of(), PyInt.of(1)),
          new Tokenize("{():1}").parseValue()
        );
        assertEquals(
          PyDict.of(
            PyTuple.of(), PyInt.of(1),
            PyTuple.of(PyInt.of(1)), PyInt.of(2)),
          new Tokenize("{():1, (1,): 2}").parseValue()
        );
    }

    @Test
    public void purePyDictListAsKey() {
        var e1 = assertThrows(RuntimeException.class, () -> {
            PyDict.of(PyList.of(), PyInt.of(1));
        });
        assertEquals("unhashable type : list", e1.getMessage());

        var e2 = assertThrows(RuntimeException.class, () -> {
            new Tokenize("{[]:1}").parseValue();
        });
        assertEquals("unhashable type : list", e2.getMessage());
    }

    @Test
    public void purePyDictDictAsKey() {
        var e1 = assertThrows(RuntimeException.class, () -> {
            PyDict.of(PyDict.of(), PyInt.of(1));
        });
        assertEquals("unhashable type : dict", e1.getMessage());

        var e2 = assertThrows(RuntimeException.class, () -> {
            new Tokenize("{{}:1}").parseValue();
        });
        assertEquals("unhashable type : dict", e2.getMessage());
    }

    @Test
    public void purePyDictAsSet() {
        assertEquals(
          PyDict.of(PyInt.of(1), None, PyInt.of(2), None),
          new Tokenize("{1,2}").parseValue()
        );
    }

    @Test
    public void purePyDictWithTailComma() {
        assertEquals(
          PyDict.of(PyInt.of(1), PyInt.of(1)),
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
          new PyStrDict(List.of("a"),
            List.of(PyList.of(PyTuple.of(PyInt.of(123))))),
          new Tokenize("{a: [(123,)]}").parseValue()
        );
    }

    @Test
    public void structureInStr() {
        assertEquals(
          PyStr.of("[]"),
          new Tokenize("'[]'").parseValue()
        );
        assertEquals(
          PyStr.of("[()]"),
          new Tokenize("'[()]'").parseValue()
        );
    }

    @Test
    public void parseLine() {
        assertEquals(List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyIndexParameter(1, "2", 2, PyInt.of(2)),
            new PyIndexParameter(2, "3", 4, PyInt.of(3))),
          new Tokenize("1,2,3").parse().values
        );
    }

    @Test
    public void parseLineWithNullValue() {
        assertEquals(
          List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyIndexParameter(1, "", 2, null),
            new PyIndexParameter(2, "3", 3, PyInt.of(3))),
          new Tokenize("1,,3").parse().values
        );
        assertEquals(
          List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyIndexParameter(1, "2", 2, PyInt.of(2)),
            new PyIndexParameter(2, "", 4, null)),
          new Tokenize("1,2,").parse().values
        );
        assertEquals(
          List.of(
            new PyIndexParameter(0, "", 0, null),
            new PyIndexParameter(1, "", 1, null),
            new PyIndexParameter(2, "", 2, null)),
          new Tokenize(",, ").parse().values
        );
        assertEquals(
          List.of(
            new PyIndexParameter(0, "", 0, null),
            new PyIndexParameter(1, "", 1, null),
            new PyIndexParameter(2, "", 2, null)),
          new Tokenize(",,").parse().values
        );
    }

    @Test
    public void parseLineWithName() {
        assertEquals(List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyNamedParameter("a", "2", 4, PyInt.of(2)),
            new PyNamedParameter("b", "3", 8, PyInt.of(3))),
          new Tokenize("1,a=2,b=3").parse().values
        );
    }

    @Test
    public void parseLineWithNameAndNullValue() {
        assertEquals(List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyNamedParameter("a", "", 4, null),
            new PyNamedParameter("b", "3", 7, PyInt.of(3))),
          new Tokenize("1,a=,b=3").parse().values
        );

        assertEquals(List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyNamedParameter("a", "2", 4, PyInt.of(2)),
            new PyNamedParameter("b", "", 8, null)),
          new Tokenize("1,a=2,b=").parse().values
        );
    }

    @Test
    public void parseLineWithUnresolvedToken() {
        assertEquals(List.of(
            new PyIndexParameter(0, "1", 0, PyInt.of(1)),
            new PyIndexParameter(1, "1+1", 2, new PyToken("1+1")),
            new PyIndexParameter(2, "1+[1]", 6, new PyToken("1+[1]")),
            new PyIndexParameter(3, "()+[]", 12, new PyToken("()+[]"))),
          new Tokenize("1,1+1,1+[1],()+[]").parse().values
        );
    }
}
