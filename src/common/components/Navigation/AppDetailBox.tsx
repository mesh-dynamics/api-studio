import React, { MouseEvent } from "react";
import { IAppDetails } from "../../reducers/state.types";
import classNames from "classnames";

export interface IAppDetailBoxState {}
export interface IAppDetailBoxProps {
  onDeleteApp: (app: IAppDetails) => void;
  onAppSelect: (app: IAppDetails) => void;
  onAppEdit: (app: IAppDetails) => void;
  app: IAppDetails;
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
            title={this.props.app.fileName}
            alt={this.props.app.fileName}
            src={`${this.props.app.filePath}`}
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
