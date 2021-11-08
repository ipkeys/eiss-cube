import { Layout } from 'react-admin';
import AppBar from './AppBar';

const CustomLayout = (props: any) => (
    <Layout 
        {...props} 
        appBar={AppBar}
    />
);

export default CustomLayout;
