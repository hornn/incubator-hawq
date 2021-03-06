package com.pivotal.pxf.plugins.hive;

import com.pivotal.pxf.api.FilterParser;
import com.pivotal.pxf.api.utilities.InputData;

import java.util.LinkedList;
import java.util.List;

/**
 * Uses the filter parser code to build a filter object,
 * either simple - a single {@link com.pivotal.pxf.api.FilterParser.BasicFilter} 
 * object or a compound - a {@link java.util.List} of
 * {@link com.pivotal.pxf.api.FilterParser.BasicFilter} objects.
 * {@link com.pivotal.pxf.plugins.hive.HiveAccessor} will use the filter 
 *        for partition filtering
 */
public class HiveFilterBuilder implements FilterParser.FilterBuilder {
    private InputData inputData;

	/**
	 * Constructs a HiveFilterBuilder object
	 * @param input
	 */
    public HiveFilterBuilder(InputData input) {
        inputData = input;
    }

    /**
     * Translates a filterString into a FilterParser.BasicFilter or a list of such filters
	 * @param filterString the string representation of the filter
	 * @return a single {@link com.pivotal.pxf.api.FilterParser.BasicFilter} object or
	 * a {@link java.util.List} of {@link com.pivotal.pxf.api.FilterParser.BasicFilter} objects.
     */
    public Object getFilterObject(String filterString) throws Exception {
        FilterParser parser = new FilterParser(this);
        Object result = parser.parse(filterString);

        if (!(result instanceof FilterParser.BasicFilter) && !(result instanceof List)) {
            throw new Exception("String " + filterString + " resolved to no filter");
        }

        return result;
    }

	@SuppressWarnings("unchecked")
    public Object build(FilterParser.Operation opId,
                        Object leftOperand,
                        Object rightOperand) throws Exception {
        if (leftOperand instanceof FilterParser.BasicFilter || leftOperand instanceof List) {
            if (opId != FilterParser.Operation.HDOP_AND ||
                    !(rightOperand instanceof FilterParser.BasicFilter)) {
                throw new Exception("Only AND is allowed between compound expressions");
            }

            if (leftOperand instanceof List) {
                return handleCompoundOperations((List<FilterParser.BasicFilter>) leftOperand, (FilterParser.BasicFilter) rightOperand);
            } else {
                return handleCompoundOperations((FilterParser.BasicFilter) leftOperand, (FilterParser.BasicFilter) rightOperand);
            }
        }

        if (!(rightOperand instanceof FilterParser.Constant)) {
            throw new Exception("expressions of column-op-column are not supported");
        }

        // Assume column is on the left
        return handleSimpleOperations(opId,
                (FilterParser.ColumnIndex) leftOperand,
                (FilterParser.Constant) rightOperand);
    }

    /*
     * Handles simple column-operator-constant expressions
     * Creates a special filter in the case the column is the row key column
     */
    private FilterParser.BasicFilter handleSimpleOperations(FilterParser.Operation opId,
                                                            FilterParser.ColumnIndex column,
                                                            FilterParser.Constant constant) {
        return new FilterParser.BasicFilter(opId, column, constant);
    }

    /*
     * Handle AND of already calculated expressions
     * Currently only AND, in the future OR can be added
     *
     * Four cases here:
     * 1) both are simple filters
     * 2) left is a FilterList and right is a filter
     * 3) left is a filter and right is a FilterList
     * 4) both are FilterLists
     *
     * Currently, 1, 2 can occur, since no parenthesis are used
     */
    private List<FilterParser.BasicFilter> handleCompoundOperations(List<FilterParser.BasicFilter> left, FilterParser.BasicFilter right) {
        left.add(right);
        return left;
    }

    private List<FilterParser.BasicFilter> handleCompoundOperations(FilterParser.BasicFilter left, FilterParser.BasicFilter right) {
        List<FilterParser.BasicFilter> result = new LinkedList<FilterParser.BasicFilter>();

        result.add(left);
        result.add(right);

        return result;
    }
}
