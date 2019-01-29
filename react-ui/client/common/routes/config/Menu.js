import React from 'react'
import { GroupMenuItem, MenuItem } from '../../components/SideBar'

const Menu = (
  <GroupMenuItem title="Configure" icon="cog" to="/config">
    <MenuItem title="Enroll NE" to="/config/enrollne" />
    <MenuItem title="Configure Node Monitoring" to="/config/nodmon" />
    <MenuItem title="Configure Log Collection" to="/config/logs" />
  </GroupMenuItem>
)

export default Menu