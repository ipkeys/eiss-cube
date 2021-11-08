import {
    useRecordContext
} from 'react-admin';
import find from 'lodash/find';
import { cmds} from '../commands';

const NavCommandTitle = (props: any) => {
    const { title } = props;
    const record = useRecordContext(props);
    const cmd = record && record.command && find(cmds, { 'id': record.command });

    return (
        <>
        {title} {cmd && cmd.name && `:: ${cmd.name}`}
        </>
    )
};

export default NavCommandTitle; 
