import React, { useEffect } from 'react';
import { Button, Glyphicon } from 'react-bootstrap';
import Shortcuts from '../../../utils/Shortcuts';
export interface IRunButtonProps{
    handleClick: any;
    requestRunning: boolean;
}
export default function RunButton(props: IRunButtonProps){

    useEffect(()=>{
        Shortcuts.register(["ctrl+r","command+r", "ctrl+enter"], props.handleClick);
        return ()=>{
            Shortcuts.unregister(["ctrl+r","command+r", "ctrl+enter"]);
        }
    }, [])
    
    return (<Button className="cube-btn text-center" onClick={props.handleClick}>
        {props.requestRunning ? <><i className="fa fa-spinner fa-spin"></i> STOP</>: <><Glyphicon glyph="play" /> RUN</>} 
        </Button>)
}