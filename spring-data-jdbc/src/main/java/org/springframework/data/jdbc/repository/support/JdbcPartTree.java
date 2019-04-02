package org.springframework.data.jdbc.repository.support;

import org.springframework.data.repository.query.parser.PartTree;

import java.util.List;

/**
 * Dedicated derived class from {@link PartTree} to implement
 * specific Spring Data JDBC functionality.
 * @see <a href="https://jira.spring.io/browse/DATAJDBC-318">Spring Data JDBC 319</a>
 * @author Thomas Lang
 */
final class JdbcPartTree extends PartTree {

    /**
     * Overloaded constructor.
     * Builds the needed references.
     * @param source a given source
     * @param domainClass a given domain class
     */
    JdbcPartTree(String source, Class<?> domainClass) {
        super(source, domainClass);
        if(this.isFatherChildMode(domainClass)) throw new IllegalArgumentException("Derived Queries do only work in simple mode, which is from Aggregate Root to Child!");
    }

    private boolean isFatherChildMode(Class<?> domainClass){
        // get iterator from getProperty
        // sollte genau ein element haben
        List<? extends Class<?>> classes = this.getParts().map(s -> s.getProperty().getLeafProperty().getOwningType().getType()).toList();

        final boolean mixedMode = classes.stream().anyMatch(s -> !s.isAssignableFrom(domainClass));
        return mixedMode;
    }

}
