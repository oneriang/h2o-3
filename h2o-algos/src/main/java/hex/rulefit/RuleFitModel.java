package hex.rulefit;

import hex.*;
import hex.glm.GLMModel;
import hex.tree.SharedTreeModel;
import hex.util.LinearAlgebraUtils;
import water.*;
import water.fvec.Frame;
import water.udf.CFuncRef;
import water.util.TwoDimTable;


public class RuleFitModel extends Model<RuleFitModel, RuleFitModel.RuleFitParameters, RuleFitModel.RuleFitOutput> {
    public enum Algorithm {DRF, GBM, AUTO}

    public enum ModelType {RULES, RULES_AND_LINEAR, LINEAR}

    @Override
    public ToEigenVec getToEigenVec() {
        return LinearAlgebraUtils.toEigen;
    }

    GLMModel glmModel;

    RuleEnsemble ruleEnsemble;
    
    public static class RuleFitParameters extends Model.Parameters {
        public String algoName() {
            return "RuleFit";
        }

        public String fullName() {
            return "RuleFit";
        }

        public String javaName() {
            return RuleFitModel.class.getName();
        }

        @Override
        public long progressUnits() {
            return RuleFit.WORK_TOTAL;
        }

        // the algorithm to use to generate rules. Options are "DRF" (default), "GBM"
        public Algorithm _algorithm = Algorithm.AUTO;

        // minimum length of rules. Defaults to 1.
        public int _min_rule_length = 1;

        // maximum length of rules. Defaults to 10.
        public int _max_rule_length = 10;

        // the maximum number of rules to return. Defaults to -1 which means the number of rules is selected 
        // by diminishing returns in model deviance.
        public int _max_num_rules = -1;

        // specifies type of base learners in the ensemble. Options are RULES_AND_LINEAR (initial ensemble includes both rules and linear terms, default), RULES (prediction rules only), LINEAR (linear terms only)
        public ModelType _model_type = ModelType.RULES_AND_LINEAR;
    }

    public static class RuleFitOutput extends Model.Output {

        // a set of rules and coefficients

        public double[] _intercept;

        public TwoDimTable _rule_importance = null;

        Key[] treeModelsKeys = null;

        Key glmModelKey = null;

        //  feature interactions ...

        public RuleFitOutput(RuleFit b) {
            super(b);
        }
    }

    public RuleFitModel(Key<RuleFitModel> selfKey, RuleFitParameters parms, RuleFitOutput output, GLMModel glmModel, RuleEnsemble ruleEnsemble) {
        super(selfKey, parms, output);
        this.glmModel = glmModel;
        this.ruleEnsemble = ruleEnsemble;
    }

    @Override
    public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
        assert domain == null;
        switch (_output.getModelCategory()) {
            case Binomial:
                return new ModelMetricsBinomial.MetricBuilderBinomial(domain);
            case Multinomial:
                return new ModelMetricsMultinomial.MetricBuilderMultinomial(_output.nclasses(), domain);
            case Regression:
                return new ModelMetricsRegression.MetricBuilderRegression();
            default:
                throw H2O.unimpl("Invalid ModelCategory " + _output.getModelCategory());
        }
    }

    @Override
    protected double[] score0(double data[], double preds[]) {
        throw new UnsupportedOperationException("RuleFitModel doesn't support scoring on raw data. Use score() instead.");
    }

    @Override
    public Frame score(Frame fr, String destination_key, Job j, boolean computeMetrics, CFuncRef customMetricFunc) throws IllegalArgumentException {
        Frame pathsFrame = new Frame(Key.make("paths_frame" + destination_key));
        if (ModelType.RULES_AND_LINEAR.equals(this._parms._model_type) || ModelType.RULES.equals(this._parms._model_type)) {
            pathsFrame.add(ruleEnsemble.transform(fr));
        }
        if (ModelType.RULES_AND_LINEAR.equals(this._parms._model_type) || ModelType.LINEAR.equals(this._parms._model_type)) {
            Frame adaptFrm = new Frame(fr.deepCopy(null));
            adaptFrm.setNames(RuleFitUtils.getLinearNames(adaptFrm.numCols(), adaptFrm.names()));
            pathsFrame.add(adaptFrm);
        }
        GLMModel glmModel = DKV.getGet(_output.glmModelKey);
        Frame destination = glmModel.score(pathsFrame, destination_key, null, true);

        pathsFrame.remove();
        return destination;
    }

}
