import styled from "styled-components";

export const Page = styled.div`
  display: grid;
  width: 100%;
  min-height: 100vh;
  gap: ${({ theme }) => theme.spacing.lg};
  align-content: start;
  padding: ${({ theme }) => theme.spacing.lg};

  @media (max-width: 40rem) {
    padding: ${({ theme }) => theme.spacing.md};
  }
`;

export const PageHeader = styled.header`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};
  padding-bottom: ${({ theme }) => theme.spacing.md};

  > span { color: ${({ theme }) => theme.colors.primarySoft}; font-size: .68rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
  h1 { margin: 0; font-size: clamp(1.8rem, 4vw, 2.8rem); letter-spacing: -.04em; }
`;

export const Layout = styled.div`
  display: grid;
  grid-template-columns: 13rem minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing.xl};
  align-items: start;

  @media (max-width: 50rem) { grid-template-columns: 1fr; }
`;

export const Navigation = styled.nav`
  position: sticky;
  top: ${({ theme }) => theme.spacing.lg};
  display: grid;
  gap: .25rem;

  @media (max-width: 50rem) {
    position: static;
    display: flex;
    overflow-x: auto;
    padding-bottom: .2rem;
  }
`;

export const NavButton = styled.button<{ $active:boolean }>`
  position: relative;
  flex: 0 0 auto;
  border: 0;
  border-radius: ${({theme})=>theme.radius.control};
  padding: .72rem .85rem;
  background: ${({theme,$active})=>$active?theme.colors.surfaceAccent:"transparent"};
  color: ${({theme,$active})=>$active?theme.colors.primarySoft:theme.colors.textMuted};
  font: inherit;
  font-size: .78rem;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  &::before{position:absolute;top:.55rem;bottom:.55rem;left:0;width:.18rem;border-radius:${({theme})=>theme.radius.round};background:${({theme,$active})=>$active?theme.colors.primary:"transparent"};content:"";}
  &:hover{background:${({theme})=>theme.colors.surface};color:${({theme})=>theme.colors.primarySoft};}
`;

export const Content = styled.div`min-width:0;`;

export const Description = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.88rem;
  line-height: 1.6;
`;

export const Section = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};
  padding: clamp(1rem, 3vw, 2rem);
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surface};
`;

export const SectionHeader = styled.header`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const SectionIcon = styled.span`
  display: grid;
  width: 2.7rem;
  height: 2.7rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
  line-height: 0;

  svg {
    display: block;
  }
`;

export const Title = styled.h2`
  margin: 0 0 0.2rem;
  font-size: 1.05rem;
`;

export const ProfileContent = styled.div`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: ${({ theme }) => theme.spacing.lg};

  @media (max-width: 36rem) {
    grid-template-columns: 1fr;
  }
`;

export const ProfileDetails = styled.div`display:grid;gap:${({theme})=>theme.spacing.lg};`;

export const DataGrid = styled.dl`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.sm};
  margin: 0;

  @media (max-width: 42rem) {
    grid-template-columns: 1fr;
  }
`;

export const DataItem = styled.div`
  min-width: 0;
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surface};

  span,
  strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    margin-bottom: 0.3rem;
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.72rem;
  }

  strong {
    font-size: 0.86rem;
  }
`;

export const Status = styled.span`
  padding: 0.35rem 0.65rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  border-color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.72rem;
  font-weight: 700;
`;


export const PreferenceGrid = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:${({theme})=>theme.spacing.md};@media(max-width:42rem){grid-template-columns:1fr;}`;
export const PreferenceField = styled.label`display:grid;gap:${({theme})=>theme.spacing.sm};padding:${({theme})=>theme.spacing.md};border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surface};span{color:${({theme})=>theme.colors.textMuted};font-size:.74rem;}strong{font-size:.88rem;}`;
export const Select = styled.select`width:100%;border:1px solid ${({theme})=>theme.colors.lineStrong};border-radius:${({theme})=>theme.radius.control};padding:.72rem .8rem;background:${({theme})=>theme.colors.surfaceStrong};color:${({theme})=>theme.colors.text};font:inherit;font-size:.8rem;cursor:pointer;`;
