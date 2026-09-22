package lithej.demos.students;

import lithej.core.Validate;

/**
 * An immutable student record: name and a grade out of 100.
 */
record StudentRecord(String name, int grade) {

    StudentRecord {
        Validate.notBlank(name, "name");
        Validate.range(grade, 0, 100, "grade");
    }

    boolean passing() {
        return grade >= 60;
    }

    @Override
    public String toString() {
        return name + ": " + grade + (passing() ? " (pass)" : " (fail)");
    }
}
