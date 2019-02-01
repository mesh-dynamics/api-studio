import React from 'react'
import { GroupMenuItem, MenuItem } from '../../components/SideBar'

const Menu = (
  <GroupMenuItem title="Alerts" icon="exclamation-triangle" to="/alerts">
    <MenuItem title="System Alerts" to="/alerts/sysalerts" />
    <MenuItem title="Analytics" to="/alerts/analytics" />
  </GroupMenuItem>
)

export default Menu