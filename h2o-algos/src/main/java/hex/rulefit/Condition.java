package hex.rulefit;

import water.Iced;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.NewChunk;
import water.fvec.Vec;

public class Condition extends Iced {
    public enum Type {Categorical, Numerical};
    public enum Operator {LessThan, GreaterThanOrEqual, In}
    int featureIndex;
    Type type;
    public Operator operator;
    public String featureName;
    public boolean NAsIncluded;
    public String languageCondition;
    public double numTreshold;
    public String[] catTreshold;

    public Condition(int featureIndex, Type type, Operator operator, double numTreshold, String[] catTreshold, String featureName, boolean NAsIncluded) {
        this.featureIndex = featureIndex;
        this.type = type;
        this.operator = operator;
        this.featureName = featureName;
        this.NAsIncluded = NAsIncluded;
        this.numTreshold = numTreshold;
        this.catTreshold = catTreshold;
        
        this.languageCondition = constructLanguaageCondition();
    }
    
    String constructLanguaageCondition() {
        String description = "(" + this.featureName;
        
        if (Operator.LessThan.equals(this.operator)) {
            description += " < ";
            description += this.numTreshold;
        } else if (Operator.GreaterThanOrEqual.equals(this.operator)) {
            description += " >= ";
            description += this.numTreshold;
        } else if (Operator.In.equals(this.operator)) {
            description += " in {";
            for (int i = 0; i < catTreshold.length; i++) {
                description += catTreshold[i];
                if (i != catTreshold.length - 1)
                    description += ", ";
            }
            description += "}";
        }
        if (this.NAsIncluded) {
            description += " or " + this.featureName + " is NA)";
        } else {
            description += ")";
        }
        return description;
    }
    
    public Frame transform(Frame frame) {
        ConditionConverter mrtask = new ConditionConverter(this);
        return mrtask.doAll(1, Vec.T_NUM, frame).outputFrame();
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return this.languageCondition.hashCode();
    }

    class ConditionConverter extends MRTask<ConditionConverter> {
        Condition _condition;
        public ConditionConverter(Condition condition) { _condition = condition; }

        @Override public void map(Chunk[] cs, NewChunk[] ncs) {

            Chunk col = cs[_condition.featureIndex];
            for (int iRow = 0; iRow < col._len; ++iRow) {
                int newVal = 0;
                // check whether condition is fulfilled:
                if (_condition.NAsIncluded) {
                    if (col.isNA(iRow))
                        newVal = 1;
                }
                if (Condition.Type.Numerical.equals(_condition.type)) {
                    if (Condition.Operator.LessThan.equals(_condition.operator)) {
                        if (col.atd(iRow) < _condition.numTreshold) {
                            newVal = 1;
                        }
                    } else if (Condition.Operator.GreaterThanOrEqual.equals(_condition.operator)) {
                        if (col.atd(iRow) >= _condition.numTreshold) {
                            newVal = 1;
                        }
                    }
                } else if (Condition.Type.Categorical.equals(_condition.type)) {
                    for (int i = 0; i < _condition.catTreshold.length; i++) {
                        if (_condition.catTreshold[i].equals(col.vec().stringAt(iRow)))
                            newVal = 1;
                    }
                }
                ncs[0].addNum(newVal);
            }
        }
    }
}
