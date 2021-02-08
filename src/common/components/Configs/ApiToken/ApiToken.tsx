import React, { useEffect, useState } from "react";
import {
    ControlLabel,
    Col,
    FormGroup,
    Form,
    FormControl,
} from "react-bootstrap";
import {
    configsService
  } from "../../../services/configs.service";
import "./ApiToken.css";

function ApiToken() {

    const [apiToken, setApiToken] = useState("");

    useEffect(() => {
        configsService
            .getApiToken()
            .then((response: any) => {
                setApiToken(response?.token);
            })
            .catch((error) => {
                console.error(error);
                setApiToken("");
            });
    }, []);

    return (
        <div className="apiToken-config-section">
            <Form horizontal>
                <FormGroup controlId="formHorizontalEmail">
                    <Col componentClass={ControlLabel} sm={1}>
                        API Token
                    </Col>
                    <Col sm={6}>
                        <FormControl componentClass="textarea" placeholder="API Token" readOnly value={apiToken} style={{height: "99px"}} />
                    </Col>
                </FormGroup>
            </Form>
        </div>
    );
}
  
export default ApiToken;