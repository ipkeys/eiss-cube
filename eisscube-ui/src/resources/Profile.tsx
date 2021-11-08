import { useGetIdentity, Loading } from 'react-admin';
import { Redirect } from 'react-router-dom';

const Profile = () => {
    const { identity, loading } = useGetIdentity();

    if (loading) {
        return <Loading />;
    }

    if (identity && identity.id) {
        return <Redirect to={`/users/${identity.id}/show`} />;
    } else {
        return <></>;
    }
}

export default Profile;
