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

import React from "react";
export interface IErrorBoundryProps {
  fallbackUI?: React.ReactNode;
}
export interface IErrorBoundryState {
  hasError: boolean;
  error: any;
  info: any;
}

export default class ErrorBoundary extends React.Component<
  IErrorBoundryProps,
  IErrorBoundryState
> {
  
  constructor(props: IErrorBoundryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      info: null
    };
  }

  componentDidCatch(error: any, errorInfo: any) {
    // You can also log the error to an error reporting service
    console.error(error, errorInfo);
    this.setState({
        hasError: true,
        error: error,
        info: errorInfo
      });
  }

  render() {
    if (this.state.hasError) {
      // You can render any custom fallback UI
      if (this.props.fallbackUI) {
        return this.props.fallbackUI;
      } else {
        return <h1>Something went wrong.</h1>;
      }
    }

    return this.props.children;
  }
}
