import { Layout } from "react-admin";
import EissAppBar from "./EissAppBar";

const EissLayout = (props: any) => (
	<Layout appBar={EissAppBar} {...props} />
);

export default EissLayout;
