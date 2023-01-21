import {
	useRecordContext
} from 'react-admin';

const NavReportTitle = (props: any) => {
	const { title } = props;
	const record = useRecordContext(props);

	return (
		<>
		{title} {record && record.cubeName && ` - ${record.cubeName}`}
		</>
	)
};

export default NavReportTitle;
