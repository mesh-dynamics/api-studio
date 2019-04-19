import React, { Component } from 'react'
import { Row, Col, Clearfix } from 'react-bootstrap'
import { XPanel, PageTitle } from '../../components'
import { connect } from 'react-redux';

class alertSample extends Component {
  constructor (props) {
    super(props)
    this.state = {
      panelVisible: true
    }
  }
  
  componentDidMount () {
    const { dispatch, user } = this.props;
  }

  render () {
    const { panelVisible } = this.state
    const onHide = e => this.setState({panelVisible: !panelVisible})
    const { user} = this.props;

    return (
      <div>
        <PageTitle title="Alert Sample" />
        <Clearfix />
        <Row>
          <Col md={12} sm={12} xs={12}>
            <XPanel visible={panelVisible} onHide={onHide}>
              <XPanel.Title title="Insight #1" smallTitle="Alert Sample">
                  <XPanel.MenuItem>Settings 1</XPanel.MenuItem>
                  <XPanel.MenuItem>Settings 2</XPanel.MenuItem>
              </XPanel.Title>
              <XPanel.Content>
                <h2>Hello {user.user} !</h2><br/>
                
                <Clearfix />
              </XPanel.Content>
            </XPanel>
          </Col>
        </Row>
      </div>
    )
  }
}




function mapStateToProps(state) {
  const { user } = state.authentication;
  return {
    user,
  }
}

const connectedSampleAlert = connect(mapStateToProps)(alertSample);

export default connectedSampleAlert