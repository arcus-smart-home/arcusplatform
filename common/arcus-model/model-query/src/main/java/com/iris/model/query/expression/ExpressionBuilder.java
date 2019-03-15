/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.iris.model.query.expression;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;
import com.iris.model.query.expression.ExpressionBuilder.ComparisonExpressionBuilder;

/**
 * 
 */
public class ExpressionBuilder {

   public static NestedExpressionBuilder create() {
      return new NestedExpressionBuilder(null);
   }
   
   protected NestedExpressionBuilder parent;
   
   protected ExpressionBuilder(NestedExpressionBuilder parent) {
      this.parent = parent;
   }
   
   protected NestedExpressionBuilder getParent() {
      if(parent == null) {
         throw new UnsupportedOperationException("Not applicable on the root expression");
      }
      return parent;
   }
   
   protected Predicate<Model> doBuild() {
      throw new UnsupportedOperationException("Can't build the root expression this way");
   }
   
   public ExpressionBuilder closeExpression() {
      NestedExpressionBuilder parent = getParent();
      parent.addExpression(this);
      return parent;
   }
   
   public Predicate<Model> build() {
      return doBuild();
   }
   
   public static class NestedExpressionBuilder extends ExpressionBuilder {
      private ExpressionBuilder delegate;
      
      private NestedExpressionBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }
      
      protected void addExpression(ExpressionBuilder delegate) {
         if(this.delegate != null) {
            throw new IllegalStateException(this + " only supports a single expression");
         }
         this.delegate = delegate;
      }
      
      protected ExpressionBuilder getDelegate() {
         if(delegate == null) {
            throw new IllegalStateException("No nested expression has been defined, can't build expression");            
         }
         return delegate;
      }
      
      /* (non-Javadoc)
       * @see com.iris.model.query.expression.ExpressionBuilder#doBuild()
       */
      @Override
      protected Predicate<Model> doBuild() {
         return getDelegate().doBuild();
      }

      public NestedExpressionBuilder openExpression() {
         return new NestedExpressionBuilder(this);
      }
      
      public AttributeComparisonBuilder openAttributeExpression() {
         return new AttributeComparisonBuilder(this);
      }
      
      public ConstantComparisonBuilder openConstantExpression() {
         return new ConstantComparisonBuilder(this);
      }
      
      public NotExpressionBuilder openNotExpression() {
         return new NotExpressionBuilder(this);
      }
      
      public BinaryExpressionBuilder openBinaryExpression() {
         return new BinaryExpressionBuilder(this);
      }
      
      public NamespaceExpressionBuilder openNamespaceExpression() {
      	return new NamespaceExpressionBuilder(this);
      }
      
      @Override
      public String toString() {
         return "(" + (delegate != null ? delegate : "<not set>") + ")";
      }
   }

   public static class NotExpressionBuilder extends NestedExpressionBuilder {
      private NotExpressionBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.expression.ExpressionBuilder#doBuild()
       */
      @Override
      protected Predicate<Model> doBuild() {
         Predicate<Model> predicate = getDelegate().doBuild();
         if(isAlwaysTrue(predicate)) {
            return alwaysFalse();
         }
         else if(isAlwaysFalse(predicate)) {
            return alwaysTrue();
         }
         else {
            return com.google.common.base.Predicates.not(getDelegate().doBuild());
         }
      }
      
      @Override
      public String toString() {
         return "not " + super.toString();
      }
      
   }
   
   public static class BinaryExpressionBuilder extends NestedExpressionBuilder {
      private enum Op {
         AND, OR;
      }
      
      private ExpressionBuilder lhs;
      private Op operator;
      
      private BinaryExpressionBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.expression.ExpressionBuilder.NestedExpressionBuilder#addExpression(com.iris.model.query.expression.ExpressionBuilder)
       */
      @Override
      protected void addExpression(ExpressionBuilder delegate) {
         if(lhs == null) {
            lhs = delegate;
         }
         else {
            super.addExpression(delegate);
         }
      }
      
      public BinaryExpressionBuilder and() {
         this.operator = Op.AND;
         return this;
      }
      
      public BinaryExpressionBuilder or() {
         this.operator = Op.OR;
         return this;
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.expression.ExpressionBuilder#doBuild()
       */
      @Override
      protected Predicate<Model> doBuild() {
         Predicate<Model> leftPredicate = lhs.build();
         Predicate<Model> rightPredicate = getDelegate().doBuild();
         switch(operator) {
         case AND:
            if(isAlwaysFalse(leftPredicate) || isAlwaysFalse(rightPredicate)) {
               return alwaysFalse();
            }
            else if(isAlwaysTrue(leftPredicate)) {
               return rightPredicate;
            }
            else if(isAlwaysTrue(rightPredicate)) {
               return leftPredicate;
            }
            else {
               return com.google.common.base.Predicates.and(leftPredicate, rightPredicate);
            }
         case OR:
            if(isAlwaysTrue(leftPredicate) || isAlwaysTrue(rightPredicate)) {
               return alwaysTrue();
            }
            else if(isAlwaysFalse(leftPredicate)) {
               return rightPredicate;
            }
            else if(isAlwaysFalse(rightPredicate)) {
               return leftPredicate;
            }
            else {
               return com.google.common.base.Predicates.or(leftPredicate, rightPredicate);
            }
         default:
            throw new IllegalArgumentException("Unrecognized operator " + operator);
         }
      }
      
      @Override
      public String toString() {
         return lhs + " " + (operator == null ? "<op>" : operator) + " " + super.toString();
      }
   }

   private enum Comparison {
      EQUALS, NOTEQUALS, LIKE, CONTAINS, SUPPORTED, GREATERTHAN, LESSTHAN, GREATERTHANEQUALTO,LESSTHANEQUALTO;
   }

   public static abstract class ComparisonExpressionBuilder extends ExpressionBuilder {
      
      protected Comparison operator;
      
      private ComparisonExpressionBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }
      
      public ComparisonExpressionBuilder equals() {
         this.operator = Comparison.EQUALS;
         return this;
      }
      
      public ComparisonExpressionBuilder notEquals() {
         this.operator = Comparison.NOTEQUALS;
         return this;
      }

      public ComparisonExpressionBuilder like() {
         this.operator = Comparison.LIKE;
         return this;
      }

      public ComparisonExpressionBuilder isSupported() {
         this.operator = Comparison.SUPPORTED;
         return this;
      }
      
      public ComparisonExpressionBuilder greaterThan() {
         this.operator = Comparison.GREATERTHAN;
         return this;
      }

      public ComparisonExpressionBuilder greaterThanEqualTo() {
         this.operator = Comparison.GREATERTHANEQUALTO;
         return this;
      }

      public ComparisonExpressionBuilder lessThan() {
         this.operator = Comparison.LESSTHAN;
         return this;
      }

      public ComparisonExpressionBuilder lessThanEqualTo() {
         this.operator = Comparison.LESSTHANEQUALTO;
         return this;
      }

      public ComparisonExpressionBuilder contains() {
         this.operator = Comparison.CONTAINS;
         return this;
      }

      public abstract ComparisonExpressionBuilder withAttributeName(String name);
      
      public abstract ComparisonExpressionBuilder withValue(Object value);
   }

   public static class ConstantComparisonBuilder extends ComparisonExpressionBuilder {
      private Object lhs;
      private Object rhs;
      
      private ConstantComparisonBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }
      
      @Override
      public ConstantComparisonBuilder withAttributeName(String attributeName) {
         throw new IllegalArgumentException("Constant expression doesn't support attribute names");
      }
      
      @Override
      public ConstantComparisonBuilder withValue(Object value) {
         if(lhs == null) {
            lhs = value;
         }
         else {
            rhs = value;
         }
         return this;
      }
      
      /* (non-Javadoc)
       * @see com.iris.model.query.expression.ExpressionBuilder#doBuild()
       */
      @SuppressWarnings({ "rawtypes", "unchecked" })
      @Override
      protected Predicate<Model> doBuild() {
         if(lhs != null && lhs instanceof Boolean && operator == null) {
            return always((Boolean) lhs);
         }
         
         switch(operator) {
         case EQUALS:
            return always(Objects.equals(lhs, rhs)); 
         case NOTEQUALS:
            return always(!Objects.equals(lhs, rhs));
         case LIKE:
            assertString(lhs);
            assertString(rhs);
            return always(Pattern.matches((String) rhs, (String) lhs));
         case GREATERTHAN:
            assertNumeric(lhs);
            assertNumeric(rhs);
            return always(((Comparable) lhs).compareTo(rhs) > 0);
         case GREATERTHANEQUALTO:
            assertNumeric(lhs);
            assertNumeric(rhs);
            return always(((Comparable) lhs).compareTo(rhs) >= 0);
         case LESSTHAN:
            assertNumeric(lhs);
            assertNumeric(rhs);
            return always(((Comparable) lhs).compareTo(rhs) < 0);
         case LESSTHANEQUALTO:
            assertNumeric(lhs);
            assertNumeric(rhs);
            return always(((Comparable) lhs).compareTo(rhs) <= 0);
         case CONTAINS:
         case SUPPORTED:
            throw new IllegalArgumentException("Can't use this comparison without an attribute");
         default:
            throw new IllegalArgumentException("Unrecognized operator " + operator);
         }
      }

      private void assertNumeric(Object o) {
         Preconditions.checkArgument(o != null, "value must be a number");
         Preconditions.checkArgument(o instanceof Number, "[%s] must be a number to apply comparisons", o);
         Preconditions.checkArgument(o instanceof Comparable, "[%s] must be comparable to apply comparisons", o);
      }

      private void assertString(Object o) {
         Preconditions.checkArgument(o != null, "value must be a string");
         Preconditions.checkArgument(o instanceof String, "[%s] must be a string", o);
      }

      @Override
      public String toString() {
         return (lhs == null ? "<lhs>" : lhs) + " " + (operator == null ? "<op>" : operator) + (rhs == null ? "<rhs>" : rhs);
      }
   }

   public static class AttributeComparisonBuilder extends ComparisonExpressionBuilder {
      private String name;
      private Object value;
      
      private AttributeComparisonBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }
      
      @Override
      public AttributeComparisonBuilder withAttributeName(String attributeName) {
         this.name = attributeName;
         return this;
      }
      
      @Override
      public AttributeComparisonBuilder withValue(Object attributeValue) {
         this.value = attributeValue;
         return this;
      }
      
      /*
       * If the name has not been set yet, then the name is on the
       * right hand side and the comparison needs to be flipped
       */
      
      @Override
      public ComparisonExpressionBuilder greaterThan() {
         return name != null ? super.greaterThan() : super.lessThan();
      }

      @Override
      public ComparisonExpressionBuilder greaterThanEqualTo() {
         return name != null ? super.greaterThanEqualTo() : super.lessThanEqualTo();
      }

      @Override
      public ComparisonExpressionBuilder lessThan() {
         return name != null ? super.lessThan() : super.greaterThan();
      }

      @Override
      public ComparisonExpressionBuilder lessThanEqualTo() {
         return name != null ? super.lessThanEqualTo() : super.greaterThanEqualTo();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.expression.ExpressionBuilder#doBuild()
       */
      @Override
      protected Predicate<Model> doBuild() {
         switch(operator) {
         case EQUALS:
            return Predicates.attributeEquals(name, value);
         case NOTEQUALS:
            return Predicates.attributeNotEquals(name, value);
         case LIKE:
            return Predicates.attributeLike(name, (String) value);
         case CONTAINS:
            return Predicates.attributeContains(name, value);
         case SUPPORTED:
            return Predicates.supportsAttribute(name);
         case GREATERTHAN:
            return Predicates.attributeGreaterThan(name,value);
         case GREATERTHANEQUALTO:
            return Predicates.attributeGreaterThanEqualTo(name,value);
         case LESSTHAN:
            return Predicates.attributeLessThan(name,value);
         case LESSTHANEQUALTO:
            return Predicates.attributeLessThanEqualTo(name,value);
         default:
            throw new IllegalArgumentException("Unrecognized operator " + operator);
         }
      }

      @Override
      public String toString() {
         return name + " " + (operator == null ? "<op>" : operator) + (operator != Comparison.SUPPORTED ? " " + value : "");
      }
   }

   public static class NamespaceExpressionBuilder extends ExpressionBuilder {
      private enum Operator {
         IS, HAS;
      }
      
      private Operator operator;
      private String namespace;
      
      private NamespaceExpressionBuilder(NestedExpressionBuilder parent) {
         super(parent);
      }
      
      public NamespaceExpressionBuilder isA() {
      	this.operator = Operator.IS;
      	return this;
      }
      
      public NamespaceExpressionBuilder hasA() {
      	this.operator = Operator.HAS;
      	return this;
      }

		public NamespaceExpressionBuilder withNamespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

      @Override
		protected Predicate<Model> doBuild() {
         switch(operator) {
         case IS:
            return Predicates.isA(namespace);
         case HAS:
            return Predicates.hasA(namespace);
         default:
            throw new IllegalArgumentException("Unrecognized operator " + operator);
         }
		}

		@Override
      public String toString() {
         return (operator == null ? "<op>" : operator) + namespace;
      }

   }
   
   private static boolean isAlwaysTrue(Predicate<Model> p) {
      return alwaysTrue().equals(p);
   }
   
   private static boolean isAlwaysFalse(Predicate<Model> p) {
      return alwaysFalse().equals(p);
   }
   
   private static Predicate<Model> alwaysTrue() {
      return com.google.common.base.Predicates.<Model>alwaysTrue();
   }

   private static Predicate<Model> alwaysFalse() {
      return com.google.common.base.Predicates.<Model>alwaysFalse();
   }

   private static Predicate<Model> always(boolean value) {
      return value ? alwaysTrue() : alwaysFalse();
   }
   
}

