import React from 'react';
import { Formik } from 'formik';

const EditTemplate = () => (
    <div>
        <Formik
            initialValues={{ presence: 'optional', type: 'exact', dataTranformationType: 'replace', matchType: 'exact' }}
            validate={values => {
                let errors = {};
                if (!values.presence) {
                    errors.presence = 'Required';
                }
                if (!values.type) {
                    errors.type = 'Required';
                }
                if (!values.dataTranformationType) {
                    errors.dataTranformationType = 'Required';
                }
                if (!values.matchType) {
                    errors.matchType = 'Required';
                }
                return errors;
            }}
            onSubmit={(values, { setSubmitting }) => {
                // FIX: throttling temporarily. will come back to it again.
                setTimeout(() => {
                    let jsonBody = JSON.stringify(values, null);
                    setSubmitting(false);
                }, 400);
            }}
        >
            {({
                values,
                errors,
                touched,
                handleChange,
                handleBlur,
                handleSubmit,
                isSubmitting,
                /* and other goodies */
            }) => (
                    <div>
                        <form className="form-horizontal" onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label className="col-sm-4 control-label">Presence</label>
                                <div className="col-sm-8">
                                    <select className="form-control"
                                        name="presence"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.presence}
                                    >
                                        <option value="required">Required</option>
                                        <option value="optional">Optional</option>
                                    </select>
                                    {errors.presence && touched.presence && (<div>{errors.presence}</div>)}
                                </div>
                            </div>
                            <div className="form-group">
                                <label className="col-sm-4 control-label">Type</label>
                                <div className="col-sm-8">
                                    <select className="form-control"
                                        name="type"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.type}
                                    >
                                        <option value="string">String</option>
                                        <option value="number">Number</option>
                                        <option value="object">Object</option>
                                        <option value="array">Array</option>
                                    </select>
                                    {errors.type && touched.type && (<div>{errors.type}</div>)}
                                </div>
                            </div>
                            <div className="form-group">
                                <label className="col-sm-4 control-label">Data transformation type</label>
                                <div className="col-sm-8">
                                    <select className="form-control"
                                        name="dataTranformationType"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.dataTranformationType}
                                    >
                                        <option value="regex">Regex</option>
                                        <option value="replace">Replace</option>
                                    </select>
                                    {errors.dataTranformationType && touched.dataTranformationType && (<div>{errors.dataTranformationType}</div>)}
                                </div>
                            </div>
                            <div className="form-group">
                                <label className="col-sm-4 control-label">Match type</label>
                                <div className="col-sm-8">
                                    <select className="form-control"
                                        name="matchType"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.matchType}
                                    >
                                        <option value="ignore">Ignore</option>
                                        <option value="exact">Exact</option>
                                    </select>
                                    {errors.matchType && touched.matchType && (<div>{errors.matchType}</div>)}
                                </div>
                            </div>
                            <div className="form-group" style={{marginTop: "12px", paddingTop: "12px", borderTop: "1px solid #eee"}}>
                                <div className="col-sm-offset-4 col-sm-8">
                                    <button type="submit" className="btn btn-default" disabled={isSubmitting}>Apply</button>
                                </div>
                            </div>
                        </form>
                    </div>

                )}
        </Formik>
    </div>
);

export default EditTemplate;