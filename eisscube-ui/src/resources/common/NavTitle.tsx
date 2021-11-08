import {
    useRecordContext
} from 'react-admin';

const NavTitle = (props: any) => {
    const { title } = props;
    const record = useRecordContext(props);

    return (
        <>
        {title} {record && record.name && `:: ${record.name}`}
        </>
    );
};

export default NavTitle; 
