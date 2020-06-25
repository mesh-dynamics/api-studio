export default {
    tree: {
        base: {
            listStyle: 'none',
            backgroundColor: 'none',
            margin: 0,
            padding: 0,
            color: '#9DA5AB',
            fontFamily: 'lucida grande ,tahoma,verdana,arial,sans-serif',
            fontSize: '14px'
        },
        node: {
            base: {
                position: 'relative'
            },
            link: {
                cursor: 'pointer',
                position: 'relative',
                padding: '0px 9px',
                display: 'flex',
                width: "260px"
            },
            activeLink: {
                background: 'none'
            },
            toggle: {
                base: {
                    position: 'relative',
                    display: 'flex',
                    verticalAlign: 'middle',
                    marginLeft: '-2px',
                    height: '24px',
                    width: '24px',
                    flexDirection: "column"
                },
                wrapper: {
                    position: 'absolute',
                    top: '8px',
                    left: '2px',
                    margin: '-7px 0 0 -7px',
                    height: '12px'
                },
                height: 9,
                width: 9,
                arrow: {
                    fill: '#9DA5AB',
                    strokeWidth: 0
                }
            },
            header: {
                base: {
                    flex: "1",
                    verticalAlign: 'middle',
                    color: '#9DA5AB'
                },
                connector: {
                    width: '2px',
                    height: '12px',
                    borderLeft: 'solid 2px black',
                    borderBottom: 'solid 2px black',
                    position: 'absolute',
                    top: '0px',
                    left: '-21px'
                },
                title: {
                    lineHeight: '24px',
                    verticalAlign: 'middle'
                }
            },
            subtree: {
                listStyle: 'none',
                paddingLeft: '19px'
            },
            loading: {
                color: '#E2C089'
            }
        }
    }
};