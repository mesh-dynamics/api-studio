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

import React, { MouseEvent } from "react";
import { IAppDetails, IAppImages } from "../../reducers/state.types";
import classNames from "classnames";

export interface IAppDetailBoxState {}
export interface IAppDetailBoxProps {
  onDeleteApp: (app: IAppDetails) => void;
  onAppSelect: (app: IAppDetails) => void;
  onAppEdit: (app: IAppDetails) => void;
  app: IAppDetails;
  image: IAppImages;
  isSelected: boolean;
}

export default class AppDetailBox extends React.PureComponent<
  IAppDetailBoxProps,
  IAppDetailBoxState
> {
  constructor(props: IAppDetailBoxProps) {
    super(props);
  }

  toggleEdit = (event: React.MouseEvent<HTMLElement, MouseEvent>) => {
    event.stopPropagation();
    this.props.onAppEdit(this.props.app);
  };

  handleDelete = (event: React.MouseEvent<HTMLElement, MouseEvent>) => {
    event.stopPropagation();
    this.props.onDeleteApp(this.props.app);
  }

  render() {
    const appBoxClass = classNames({
      "app-box": true,
      selected: this.props.isSelected,
    });

    return (
      <div
        key={this.props.app.app.id}
        className={appBoxClass}
        onClick={() => this.props.onAppSelect(this.props.app)}
      >
        <div className="app-img">
          <img
            style={{ width: "100%", height: "100%" }}
            id="base64image"
            title={this.props.image && this.props.image.fileName}
            alt={this.props.image && this.props.image.fileName}
            src={this.props.image && `data:image/jpeg;base64, ${this.props.image.data}`}
          />
        </div>
        <div style={{ width: "100%", position: "relative" }}>
          <div className="app-name">{this.props.app.app.displayName}</div>
          <span className="app-actions">
            <i className="fas pointer fa-edit" onClick={this.toggleEdit} />
            {
            !this.props.isSelected && 
              <i
                className="fas fa-trash pointer"
                onClick={this.handleDelete}
              />
            }
          </span>
        </div>
      </div>
    );
  }
}
