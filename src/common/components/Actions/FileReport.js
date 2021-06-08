import React from 'react';
import { Formik } from 'formik';

const FileReport = () => (
    <div>
        <Formik
            initialValues={{ title: '', prLink: '', affects: 'allInstances', description: '' }}
            validate={values => {
                let errors = {};
                if (!values.title) {
                    errors.title = 'Required';
                }
                if (!values.prLink) {
                    errors.prLink = 'Required';
                }
                if (!values.affects) {
                    errors.affects = 'Required';
                }
                if (!values.description) {
                    errors.description = 'Required';
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
                                <label className="col-sm-2 control-label">Title</label>
                                <div className="col-sm-10">
                                    <input type="text" className="form-control" placeholder="title" 
                                        name="title"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.title} 
                                    />
                                    {errors.title && touched.title && (<div>{errors.title}</div>)}
                                </div>
                            </div>
                            <div className="form-group">
                                <label className="col-sm-2 control-label">PR Link</label>
                                <div className="col-sm-10">
                                    <input type="text" className="form-control" placeholder="Jira Link" 
                                        name="prLink"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.prLink} 
                                    />
                                    {errors.prLink && touched.prLink && (<div>{errors.prLink}</div>)}
                                </div>
                            </div>
                            <div className="form-group">
                                <label className="col-sm-2 control-label">Affects</label>
                                <div className="col-sm-10">
                                    <select className="form-control"
                                        name="affects"
                                        onChange={handleChange}
                                        onBlur={handleBlur}
                                        value={values.affects}
                                    >
                                        <option value="allInstances">All Instances</option>
                                        <option value="currentInstance">Current Instance</option>
                                    </select>
                                    {errors.affects && touched.affects && (<div>{errors.affects}</div>)}
                                </div>
                            </div>
                            <div className="form-group">
                                <label className="col-sm-2 control-label">Description</label>
                                <div className="col-sm-10">
                                    <textarea className="form-control" rows="3"
                                            name="description"
                                            onChange={handleChange}
                                            onBlur={handleBlur}
                                            value={values.description} 
                                    >
                                    </textarea>
                                    {errors.description && touched.description && (<div>{errors.description}</div>)}
                                </div>
                            </div>
                            <div className="form-group" style={{marginTop: "12px", paddingTop: "12px", borderTop: "1px solid #eee"}}>
                                <div className="col-sm-offset-2 col-sm-10">
                                    <button type="submit" className="btn btn-default" disabled={isSubmitting}>Report</button>
                                </div>
                            </div>
                        </form>
                    </div>

                )}
        </Formik>
    </div>
);

export default FileReport;