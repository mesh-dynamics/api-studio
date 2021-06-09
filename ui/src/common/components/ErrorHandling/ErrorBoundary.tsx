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
