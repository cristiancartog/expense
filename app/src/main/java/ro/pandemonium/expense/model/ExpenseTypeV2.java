package ro.pandemonium.expense.model;

public class ExpenseTypeV2 {

    private Integer id;
    private Integer parent;
    private String name;
    private boolean isSingleOccurrence = false;
    private Integer order;

    public ExpenseTypeV2(final Integer id, final Integer parent, final String name, final boolean isSingleOccurrence, final Integer order) {
        this.id = id;
        this.parent = parent;
        this.name = name;
        this.isSingleOccurrence = isSingleOccurrence;
        this.order = order;
    }

    public Integer getId() {
        return id;
    }

    public Integer getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public boolean isSingleOccurrence() {
        return isSingleOccurrence;
    }

    public Integer getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + ", " + parent + ", " + isSingleOccurrence + ", " + order + "]";
    }
}
