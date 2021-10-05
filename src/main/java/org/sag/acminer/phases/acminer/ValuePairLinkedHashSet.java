package org.sag.acminer.phases.acminer;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

public class ValuePairLinkedHashSet extends ValuePairHashSet {
	
	public ValuePairLinkedHashSet() {
		super(true);
	}
	
	public ValuePairLinkedHashSet(Collection<ValuePair> in) {
		this();
		addAll(in);
	}
	
	/**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@code Spliterator} over the elements in this set.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#DISTINCT}, and {@code ORDERED}.  Implementations
     * should document the reporting of additional characteristic values.
     *
     * @implNote
     * The implementation creates a
     * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
     * from the set's {@code Iterator}.  The spliterator inherits the
     * <em>fail-fast</em> properties of the set's iterator.
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SUBSIZED}.
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    @Override
    public Spliterator<ValuePair> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
    
    private static final ValuePairLinkedHashSet emptySet = new ValuePairLinkedHashSet();
	
	public static ValuePairLinkedHashSet getEmptySet() {
		return emptySet;
	}

}
