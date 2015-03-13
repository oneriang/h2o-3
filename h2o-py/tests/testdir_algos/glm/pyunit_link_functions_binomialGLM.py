import sys
sys.path.insert(1, "../../../")
import h2o
import pandas as pd
import zipfile
import statsmodels.api as sm

def link_functions_binomial(ip,port):
	# Connect to h2o
	h2o.init(ip,port)

	print("Read in prostate data.")
	h2o_data = h2o.import_frame(path=h2o.locate("smalldata/prostate/prostate_complete.csv.zip"))
	h2o_data.head()

	sm_data = pd.read_csv(zipfile.ZipFile(h2o.locate("smalldata/prostate/prostate_complete.csv.zip")).open("prostate_complete.csv")).as_matrix()
	sm_data_response = sm_data[:,2].asfactor()
	sm_data_features = sm_data[:,[1,3,4,5,6,7,8,9]]

	print("Testing for family: BINOMIAL")
	print("Set variables for h2o.")
	myY = "CAPSULE"
	myX = ["ID","AGE","RACE","GLEASON","DCAPS","PSA","VOL","DPROS"]

	print("Create models with canonical link: LOGIT")
	h2o_model = h2o.glm(x=h2o_data[myX], y=h2o_data[myY], family="binomial", link="logit",alpha=[0.5], Lambda=[0], n_folds=0)
	sm_model = sm.GLM(endog=sm_data_response, exog=sm_data_features, family=sm.families.Binomial(sm.families.links.logit)).fit()

	print("Compare model deviances for link function logit")
	h2o_deviance = h2o_model._model_json['output']['residual_deviance'] / h2o_model._model_json['output']['null_deviance']
	sm_deviance = sm_model.deviance / sm_model.null_deviance
	assert h2o_deviance - sm_deviance < 0.01, "expected h2o to have an equivalent or better deviance measures"

if __name__ == "__main__":
	h2o.run_test(sys.argv, link_functions_binomial)


