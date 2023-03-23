package tastymima

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Flags.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Types.*

private[tastymima] final class TypeTranslator(oldCtx: Context, newCtx: Context):
  private val translatedBinders = new java.util.IdentityHashMap[Binders, Binders]()

  def translateType(oldType: Type): Type =
    oldType match
      case oldType: NamedType =>
        oldType.prefix match
          case oldPrefix: Type =>
            val translatedPrefix = translateType(oldPrefix)
            oldType.symbol(using oldCtx) match
              case oldSym: ClassTypeParamSymbol =>
                translateClassTypeParamRef(translatedPrefix, oldSym)
              case _ =>
                NamedType(translatedPrefix, oldType.name)(using newCtx)
          case NoPrefix =>
            throw InvalidProgramStructureException(s"Unexpected local ref $oldType")

      case oldType: PackageRef =>
        PackageRef(oldType.fullyQualifiedName)

      case oldType: ThisType =>
        ThisType(translateType(oldType.tref).asInstanceOf[TypeRef])

      case oldType: SuperType =>
        val translatedThistpe = translateType(oldType.thistpe).asInstanceOf[ThisType]
        val translatedExplicitSupertpe = oldType.explicitSupertpe.map(translateType(_))
        SuperType(translatedThistpe, translatedExplicitSupertpe)

      case oldType: ConstantType =>
        ConstantType(oldType.value)

      case oldType: AppliedType =>
        AppliedType(translateType(oldType.tycon), oldType.args.map(translateType(_)))

      case oldType: ExprType =>
        ExprType(translateType(oldType.resultType))

      case oldType: TermLambdaType =>
        oldType.companion(oldType.paramNames)(
          { lt =>
            translatedBinders.put(oldType, lt)
            oldType.paramInfos.map(translateType(_))
          },
          lt => translateType(oldType.resultType)
        )

      case oldType: TypeLambdaType =>
        oldType.companion(oldType.paramNames)(
          { lt =>
            translatedBinders.put(oldType, lt)
            oldType.paramInfos.map(translateTypeBounds(_))
          },
          lt => translateType(oldType.resultType)
        )

      case oldType: ParamRef =>
        val translatedParamBinders = translatedBinders.get(oldType.binders).nn.asInstanceOf[ParamRefBinders]
        translatedParamBinders.paramRefs(oldType.paramNum)

      case oldType: AnnotatedType =>
        // There is nothing we can do about the annotation here, so we get rid of it
        translateType(oldType.typ)

      case oldType: TypeRefinement =>
        TypeRefinement(translateType(oldType.parent), oldType.refinedName, translateTypeBounds(oldType.refinedBounds))

      case oldType: TermRefinement =>
        TermRefinement(translateType(oldType.parent), oldType.refinedName, translateType(oldType.refinedType))

      case oldType: RecType =>
        RecType({ rt =>
          translatedBinders.put(oldType, rt)
          translateType(oldType.parent)
        })

      case oldType: RecThis =>
        val translatedRecType = translatedBinders.get(oldType.binders).nn.asInstanceOf[RecType]
        translatedRecType.recThis

      case oldType: MatchTypeCase =>
        MatchTypeCase(translateType(oldType.pattern), translateType(oldType.result))

      case oldType: MatchType =>
        val translatedBound = translateType(oldType.bound)
        val translatedScrutinee = translateType(oldType.scrutinee)
        val translatedCases: List[MatchTypeCase | TypeLambda] = oldType.cases.map {
          case tpCase: MatchTypeCase => translateType(tpCase).asInstanceOf[MatchTypeCase]
          case tpCase: TypeLambda    => translateType(tpCase).asInstanceOf[TypeLambda]
        }
        MatchType(translatedBound, translatedScrutinee, translatedCases)

      case oldType: BoundedType =>
        BoundedType(translateTypeBounds(oldType.bounds), oldType.alias.map(translateType(_)))

      case oldType: NamedTypeBounds =>
        NamedTypeBounds(oldType.name, translateTypeBounds(oldType.bounds))

      case oldType: WildcardTypeBounds =>
        WildcardTypeBounds(translateTypeBounds(oldType.bounds))

      case oldType: OrType =>
        OrType(translateType(oldType.first), translateType(oldType.second))

      case oldType: AndType =>
        AndType(translateType(oldType.first), translateType(oldType.second))

      case oldType: CustomTransientGroundType =>
        throw InvalidProgramStructureException(s"Unexpected custom transient type $oldType")
  end translateType

  def translateTypeBounds(oldBounds: TypeBounds): TypeBounds = oldBounds match
    case RealTypeBounds(low, high) => RealTypeBounds(translateType(low), translateType(high))
    case TypeAlias(alias)          => TypeAlias(translateType(alias))
  end translateTypeBounds

  private def translateClassTypeParamRef(translatedPrefix: Type, oldSym: ClassTypeParamSymbol): TypeRef =
    // We know that class type param counts match because of a check in `analyzeClass`
    val typeParamIndex = withOldCtx {
      oldSym.owner.typeParams.indexOf(oldSym)
    }
    val translatedSym = withNewCtx {
      translatedPrefix match
        case translatedPrefix: ThisType =>
          translatedPrefix.cls.typeParams(typeParamIndex)
        case _ =>
          throw NotImplementedError(s"cannot translate class type param ref with non-this prefix $translatedPrefix")
    }
    TypeRef(translatedPrefix, translatedSym)
  end translateClassTypeParamRef

  private def withOldCtx[A](f: Context ?=> A): A = f(using oldCtx)

  private def withNewCtx[A](f: Context ?=> A): A = f(using newCtx)
end TypeTranslator
