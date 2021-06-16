
/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function InputController(component, field, value='') {
  init()

  return () => ({
    onChange,
    value: component.state.form[field]
  })

  function init () {
    const state = component.state || {}
    const form = state.form || { }

    form[field] = value
    state.form = form
    component.state = state
  }

  function onChange (e) {
    let value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    component.setState(prevState => {
      const form = { ...prevState.form }
      form[field] = value
      return { form }
    });
  }
}

export default InputController