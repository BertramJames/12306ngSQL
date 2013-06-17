/*
 * Copyright 2012-2013 NgSql Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ng12306.ngsql.parser.ast.fragment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.ng12306.ngsql.parser.ast.ASTNode;
import org.ng12306.ngsql.parser.ast.expression.Expression;
import org.ng12306.ngsql.parser.util.Pair;
import org.ng12306.ngsql.parser.visitor.SQLASTVisitor;


/**
 * 
* [添加说明]
* @author: <a href="mailto:lvbomr@gmail.com">lvbo</a>
* @date: 2013-5-26 上午11:37:36
* @version: 1.0
 */
public class OrderBy implements ASTNode {
    /** might be {@link LinkedList} */
    private final List<Pair<Expression, SortOrder>> orderByList;

    public List<Pair<Expression, SortOrder>> getOrderByList() {
        return orderByList;
    }

    /**
     * performance tip: linked list is used
     */
    public OrderBy() {
        this.orderByList = new LinkedList<Pair<Expression, SortOrder>>();
    }

    /**
     * performance tip: expect to have only 1 order item
     */
    public OrderBy(Expression expr, SortOrder order) {
        this.orderByList = new ArrayList<Pair<Expression, SortOrder>>(1);
        this.orderByList.add(new Pair<Expression, SortOrder>(expr, order));
    }

    public OrderBy(List<Pair<Expression, SortOrder>> orderByList) {
        if (orderByList == null) throw new IllegalArgumentException("order list is null");
        this.orderByList = orderByList;
    }

    public OrderBy addOrderByItem(Expression expr, SortOrder order) {
        orderByList.add(new Pair<Expression, SortOrder>(expr, order));
        return this;
    }

    @Override
    public void accept(SQLASTVisitor visitor) {
        visitor.visit(this);
    }
}
