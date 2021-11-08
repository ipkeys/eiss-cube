import Icon from '@material-ui/icons/Message';

export const CommandIcon = Icon;
export { default as CommandList } from './CommandList';
export { default as CommandShow } from './CommandShow';
export { default as CommandCreate } from './CommandCreate';

export const checkCommandForRelayCycle = (v: string) => {
    return (v === 'rcyc') ? true : false ;
};

export const checkCommandForInputCount = (v: string) => {
    return (v === 'icp') ? true : false ;
};

export const checkCommandForInputCycle = (v: string) => {
    return (v === 'icc') ? true : false ;
};

export const cmds = [
    { id: 'ron', name: 'Relay ON' },
    { id: 'roff', name: 'Relay OFF' },
    { id: 'rcyc', name: 'Relay CYCLE' },
    { id: 'icp', name: 'Counting PULSE' },
    { id: 'icc', name: 'Counting CYCLE' },
    { id: 'ioff', name: 'Counting STOP' },
    { id: 'reboot', name: 'REBOOT' }
];

export const edges = [
    { id: 'r', name: 'Raising edge' },
    { id: 'f', name: 'Falling edge' }
];
